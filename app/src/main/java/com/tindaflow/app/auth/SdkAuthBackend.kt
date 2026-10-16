@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.tindaflow.app.auth

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.status.RefreshFailureCause
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

/** SDK sessions and all tokens remain below this boundary. */
internal class SdkAuthBackend(context: Context, config: SupabaseConfig, private val scope: CoroutineScope) : AuthRepository {
    private val client by lazy { SupabaseClientFactory.create(context.applicationContext, config) }
    private val auth get() = client.auth
    private val storage get() = auth.sessionManager as KeystoreAuthStorage
    private val current = MutableStateFlow<AuthSession>(AuthSession.Restoring)
    override val session = current.asStateFlow()
    private var observer: Job? = null
    @Volatile private var trustedUserId: String? = null

    private suspend fun initialize() {
        auth.awaitInitialization()
        if (observer == null) observer = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.NotAuthenticated -> {
                        trustedUserId = null
                        // Initial SDK startup is not a completed restoration decision.
                        if (current.value != AuthSession.Restoring) current.value = AuthSession.SignedOut
                    }
                    is SessionStatus.RefreshFailure -> {
                        @Suppress("DEPRECATION")
                        val reason = if (status.cause is RefreshFailureCause.NetworkError) AuthFailure.NETWORK else AuthFailure.SERVER
                        current.value = AuthSession.Unavailable(reason)
                    }
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id
                        if (userId != null && userId == trustedUserId) {
                            if (status.session.expiresAt.toEpochMilliseconds() <= System.currentTimeMillis())
                                current.value = AuthSession.Unavailable(AuthFailure.SESSION_EXPIRED)
                            else try {
                                current.value = if (storage.hasRecovery(userId)) AuthSession.Recovery else AuthSession.Verified(userId)
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { current.value = AuthSession.Unavailable(AuthFailure.STORAGE) }
                        }
                    }
                    SessionStatus.Initializing -> Unit
                }
            }
        }
    }

    private suspend fun validateSession(): AuthResult {
        val cached = auth.currentSessionOrNull()
            ?: return AuthResult.SignedOut.also { current.value = AuthSession.SignedOut }
        if (cached.expiresAt.toEpochMilliseconds() <= System.currentTimeMillis()) auth.refreshCurrentSession()
        val user = auth.retrieveUserForCurrentSession(updateSession = true)
        if (user.emailConfirmedAt == null) {
            trustedUserId = null
            current.value = AuthSession.Unverified
            return AuthResult.AwaitingVerification
        }
        trustedUserId = user.id
        return if (storage.hasRecovery(user.id)) {
            current.value = AuthSession.Recovery
            AuthResult.ResettingPassword
        } else {
            current.value = AuthSession.Verified(user.id)
            AuthResult.Authenticated(user.id)
        }
    }

    private suspend fun guarded(block: suspend () -> AuthResult): AuthResult = try {
        withTimeout(25_000) { initialize(); block() }
    } catch (_: TimeoutCancellationException) {
        current.value = AuthSession.Unavailable(AuthFailure.NETWORK)
        AuthResult.Failure(AuthFailure.NETWORK)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) {
        val reason = authFailureForThrowable(error)
        if (current.value == AuthSession.Restoring || reason == AuthFailure.SESSION_EXPIRED || reason == AuthFailure.STORAGE) {
            trustedUserId = null
            current.value = AuthSession.Unavailable(reason)
        }
        AuthResult.Failure(reason)
    }

    override suspend fun checkSetup() = AuthResult.Ready
    override suspend fun restoreSession() = guarded {
        current.value = AuthSession.Restoring
        if (auth.currentSessionOrNull() == null) {
            val loaded = auth.loadFromStorage()
            if (loaded && auth.currentSessionOrNull() == null) {
                current.value = AuthSession.SignedOut
                return@guarded AuthResult.Failure(AuthFailure.SESSION_EXPIRED)
            }
        }
        validateSession()
    }
    override suspend fun checkVerification() = guarded {
        if (auth.currentSessionOrNull() == null) AuthResult.Failure(AuthFailure.EMAIL_UNVERIFIED)
        else validateSession()
    }
    override suspend fun signIn(email: String, password: String) = guarded {
        if (InputValidation.email(email) != null || InputValidation.loginPassword(password) != null)
            return@guarded AuthResult.Failure(AuthFailure.INVALID_INPUT)
        storage.clearRecovery()
        auth.signInWith(Email) { this.email = email; this.password = password }
        storage.deleteCodeVerifier()
        validateSession()
    }
    override suspend fun register(name: String, email: String, password: String) = guarded {
        if (InputValidation.businessName(name) != null || InputValidation.email(email) != null || InputValidation.password(password) != null)
            return@guarded AuthResult.Failure(AuthFailure.INVALID_INPUT)
        storage.beginFlow(PendingAuthFlow.CONFIRMATION)
        auth.signUpWith(Email, redirectUrl = SupabaseConfig.CALLBACK_URL) {
            this.email = email; this.password = password
            data = buildJsonObject { put("display_name", name) } // Display only, never authorization.
        }
        storage.pendingFlow() // Surface a failed verifier write before reporting acceptance.
        if (auth.currentSessionOrNull() != null) validateSession()
        else { current.value = AuthSession.Unverified; AuthResult.AwaitingVerification }
    }
    override suspend fun resendVerification(email: String) = guarded {
        if (InputValidation.email(email) != null) return@guarded AuthResult.Failure(AuthFailure.INVALID_INPUT)
        // SDK 3.2.3's resendEmail omits the PKCE challenge and redirect. Use its
        // existing HTTP transport with the documented Auth endpoint and error parser.
        storage.beginFlow(PendingAuthFlow.CONFIRMATION)
        val verifier = AuthPkce.verifier()
        storage.saveCodeVerifier(verifier)
        storage.pendingFlow()
        val response = client.httpClient.postJson(auth.resolveUrl("resend"), buildJsonObject {
            put("type", "signup"); put("email", email)
            put("code_challenge", AuthPkce.challenge(verifier)); put("code_challenge_method", "s256")
        }.toString()) { parameter("redirect_to", SupabaseConfig.CALLBACK_URL) }
        if (!response.status.isSuccess()) throw auth.parseErrorResponse(response)
        AuthResult.VerificationEmailRequested
    }
    override suspend fun requestRecovery(email: String) = guarded {
        if (InputValidation.email(email) != null) return@guarded AuthResult.Failure(AuthFailure.INVALID_INPUT)
        val flow = storage.beginFlow(PendingAuthFlow.RECOVERY)
        auth.resetPasswordForEmail(email, redirectUrl = SupabaseConfig.CALLBACK_URL)
        // This SDK version saves the recovery verifier in an asynchronous job.
        withTimeout(3_000) { while (storage.pendingFlow() != flow) delay(20) }
        AuthResult.RecoveryEmailRequested
    }
    override suspend fun handleCallback(url: String) = guarded {
        val callback = AuthCallback.parse(url) ?: return@guarded AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        callback.error?.let { return@guarded AuthResult.Failure(it) }
        val pending = storage.pendingFlow()
        if (pending == null || !pending.isFresh(System.currentTimeMillis()))
            return@guarded AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        // Server checks the saved PKCE verifier. Never import URL-supplied tokens.
        val exchanged = auth.exchangeCodeForSession(callback.code!!, saveSession = false)
        val user = exchanged.user ?: return@guarded AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        if (pending.flow == PendingAuthFlow.RECOVERY) storage.markRecovery(user.id) else storage.clearRecovery()
        auth.importSession(exchanged)
        validateSession()
    }
    override suspend fun resetPassword(password: String) = guarded {
        val userId = auth.currentUserOrNull()?.id
        if (current.value != AuthSession.Recovery || userId == null || !storage.hasRecovery(userId))
            return@guarded AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        if (InputValidation.password(password) != null) return@guarded AuthResult.Failure(AuthFailure.INVALID_INPUT)
        auth.updateUser { this.password = password }
        storage.clearRecovery()
        trustedUserId = userId
        current.value = AuthSession.Verified(userId)
        AuthResult.PasswordUpdated
    }
    override suspend fun signOut() = guarded {
        auth.signOut(SignOutScope.LOCAL)
        storage.clearRecovery()
        trustedUserId = null
        current.value = AuthSession.SignedOut
        AuthResult.SignedOut
    }
}
