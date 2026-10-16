package com.tindaflow.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** SDK persistence extension points. Ciphertext only in noBackupFilesDir; no plaintext fallback. */
internal class KeystoreAuthStorage(context: Context, projectUrl: String) : SessionManager, CodeVerifierCache {
    private val project = MessageDigest.getInstance("SHA-256")
        .digest(projectUrl.trimEnd('/').toByteArray()).joinToString("") { "%02x".format(it) }
    private val directory = context.noBackupFilesDir
    private val alias = "tindaflow.auth.$project"
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val cipher = AuthCipher(::loadKey)
    @Volatile private var requestedFlow: PendingAuthContext? = null
    @Volatile private var verifierFailure: AuthStorageException? = null

    suspend fun beginFlow(flow: PendingAuthFlow): PendingAuthContext {
        verifierFailure = null
        delete("pkce")
        val context = PendingAuthContext(flow, System.currentTimeMillis())
        requestedFlow = context
        return context
    }
    suspend fun pendingFlow(): PendingAuthContext? {
        verifierFailure?.let { throw it }
        val record = read("pkce")?.let { json.parseToJsonElement(it).jsonObject } ?: return null
        return PendingAuthContext(PendingAuthFlow.valueOf(record.getValue("flow").jsonPrimitive.content),
            record.getValue("created").jsonPrimitive.long)
    }
    suspend fun markRecovery(userId: String) = write("recovery", buildJsonObject {
        put("user", userId); put("expires", System.currentTimeMillis() + 30 * 60 * 1000L)
    }.toString())
    suspend fun hasRecovery(userId: String): Boolean {
        val record = read("recovery")?.let { json.parseToJsonElement(it).jsonObject } ?: return false
        return record["user"]?.jsonPrimitive?.content == userId &&
            (record["expires"]?.jsonPrimitive?.longOrNull ?: 0) > System.currentTimeMillis()
    }
    suspend fun clearRecovery() = delete("recovery")

    private fun loadKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    private fun file(purpose: String) = AtomicFile(File(directory, "auth-$project-$purpose.enc"))
    private suspend fun <T> secureStorageOperation(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { throw AuthStorageException() }

    private suspend fun write(purpose: String, value: String) = secureStorageOperation { rawWrite(purpose, value) }
    private suspend fun read(purpose: String): String? = secureStorageOperation { rawRead(purpose) }
    private suspend fun delete(purpose: String) = secureStorageOperation { rawDelete(purpose) }

    private suspend fun rawWrite(purpose: String, value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = file(purpose)
            val bytes = cipher.encrypt(value.toByteArray(Charsets.UTF_8), "$project:$purpose")
            val stream = target.startWrite()
            try {
                stream.write(bytes)
                target.finishWrite(stream)
            } catch (failure: Exception) {
                target.failWrite(stream)
                throw failure
            }
        }
    }
    private suspend fun rawRead(purpose: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = file(purpose)
            val bytes = try {
                target.readFully()
            } catch (missing: FileNotFoundException) {
                if (target.baseFile.exists()) throw missing
                return@withLock null
            }
            cipher.decrypt(bytes, "$project:$purpose").toString(Charsets.UTF_8)
        }
    }
    private suspend fun rawDelete(purpose: String) = withContext(Dispatchers.IO) {
        mutex.withLock { file(purpose).delete() }
    }

    override suspend fun saveSession(session: UserSession) = write("session", json.encodeToString(UserSession.serializer(), session))
    override suspend fun loadSession(): UserSession? = read("session")?.let { json.decodeFromString(UserSession.serializer(), it) }
    override suspend fun deleteSession() = delete("session")
    override suspend fun saveCodeVerifier(codeVerifier: String) {
        val flow = requestedFlow
        if (flow == null) { verifierFailure = AuthStorageException(); return }
        try {
            write("pkce", buildJsonObject {
                put("verifier", codeVerifier); put("flow", flow.flow.name); put("created", flow.createdAtMillis)
            }.toString())
        } catch (failure: AuthStorageException) {
            // SDK 3.2.3 writes recovery verifiers in a background job. Surface the
            // failure through the awaited repository check, not an uncaught SDK job.
            verifierFailure = failure
        }
    }
    override suspend fun loadCodeVerifier(): String? = read("pkce")?.let {
        json.parseToJsonElement(it).jsonObject.getValue("verifier").jsonPrimitive.content
    }
    override suspend fun deleteCodeVerifier() { requestedFlow = null; delete("pkce") }
}
