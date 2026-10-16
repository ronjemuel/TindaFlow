package com.tindaflow.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Configuration boundary and serialized operations; the injected backend is independently testable. */
class SupabaseAuthRepository(private val configuration: SupabaseConfig,
    backendFactory: (() -> AuthRepository)? = null) : AuthRepository {
    private val backend by lazy { backendFactory?.invoke() }
    private val emptySession = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
    override val session: StateFlow<AuthSession> get() = backend?.session ?: emptySession
    private val mutex = Mutex()
    private fun configurationFailure(): AuthFailure? = when (configuration.status) {
        AuthConfiguration.MISSING -> AuthFailure.CONFIGURATION_MISSING
        AuthConfiguration.INVALID -> AuthFailure.CONFIGURATION_INVALID
        AuthConfiguration.READY -> null
    }
    override suspend fun checkSetup(): AuthResult = configurationFailure()?.let { AuthResult.Failure(it) }
        ?: if (backend == null) AuthResult.Failure(AuthFailure.INTEGRATION_PENDING) else AuthResult.Ready
    private suspend fun call(operation: suspend (AuthRepository) -> AuthResult): AuthResult = mutex.withLock {
        configurationFailure()?.let { return@withLock AuthResult.Failure(it) }
        val target = backend ?: return@withLock AuthResult.Failure(AuthFailure.INTEGRATION_PENDING)
        try { operation(target) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { AuthResult.Failure(authFailureForThrowable(error)) }
    }
    override suspend fun restoreSession() = call { it.restoreSession() }
    override suspend fun signIn(email: String, password: String) = call { it.signIn(email, password) }
    override suspend fun register(name: String, email: String, password: String) = call { it.register(name, email, password) }
    override suspend fun resendVerification(email: String) = call { it.resendVerification(email) }
    override suspend fun requestRecovery(email: String) = call { it.requestRecovery(email) }
    override suspend fun resetPassword(password: String): AuthResult {
        if (session.value != AuthSession.Recovery) return AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        return call { it.resetPassword(password) }
    }
    override suspend fun signOut() = call { it.signOut() }
    override suspend fun handleCallback(url: String) = call { it.handleCallback(url) }
    override suspend fun checkVerification() = call { it.checkVerification() }
}
