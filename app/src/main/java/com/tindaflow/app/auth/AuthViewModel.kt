package com.tindaflow.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import java.io.IOException

/** Immutable UI state only. No passwords, raw provider messages or session tokens. */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state = mutableState.asStateFlow()
    val session = repository.session
    private val mutablePendingEmail = MutableStateFlow("")
    val pendingEmail = mutablePendingEmail.asStateFlow()
    private var started = false
    private var callbackJob: Job? = null
    private var nextVerificationResend = 0L

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            var previous: AuthSession = AuthSession.Restoring
            session.collect { current ->
                if (state.value !is AuthUiState.Loading) when (current) {
                    is AuthSession.Unavailable -> mutableState.value = AuthUiState.Error(current.reason)
                    is AuthSession.Verified -> if (state.value != AuthUiState.PasswordUpdated)
                        mutableState.value = AuthUiState.Authenticated(current.userId)
                    AuthSession.Recovery -> mutableState.value = AuthUiState.ResettingPassword
                    AuthSession.SignedOut -> if (previous is AuthSession.Verified && state.value != AuthUiState.SignedOut)
                        mutableState.value = AuthUiState.Error(AuthFailure.SESSION_EXPIRED)
                    else -> Unit
                }
                previous = current
            }
        }
        restoreSession()
    }
    fun clearAction() { if (state.value !is AuthUiState.Loading) mutableState.value = AuthUiState.Idle }
    fun handleCallback(url: String) {
        callbackJob?.cancel()
        callbackJob = viewModelScope.launch {
            state.first { it !is AuthUiState.Loading }
            perform(AuthOperation.CALLBACK) { repository.handleCallback(url) }
        }
    }
    fun checkVerification() = perform(AuthOperation.VERIFY) { repository.checkVerification() }

    fun checkSetup() = perform(AuthOperation.CHECK_SETUP) { repository.checkSetup() }
    fun restoreSession() = perform(AuthOperation.RESTORE) { repository.restoreSession() }
    fun signIn(email: String, password: String) = perform(AuthOperation.SIGN_IN) {
        mutablePendingEmail.value = email.trim()
        repository.signIn(email.trim(), password)
    }
    fun register(name: String, email: String, password: String) = perform(AuthOperation.REGISTER) {
        mutablePendingEmail.value = email.trim()
        repository.register(name.trim(), email.trim(), password)
    }
    fun resendVerification(email: String) = perform(AuthOperation.RESEND) {
        if (System.nanoTime() < nextVerificationResend) AuthResult.Failure(AuthFailure.RATE_LIMITED)
        else repository.resendVerification(email.trim()).also {
            if (it == AuthResult.VerificationEmailRequested) nextVerificationResend = System.nanoTime() + 60_000_000_000L
        }
    }
    fun requestRecovery(email: String) = perform(AuthOperation.RECOVER) { repository.requestRecovery(email.trim()) }
    fun resetPassword(password: String) = perform(AuthOperation.RESET) {
        if (session.value != AuthSession.Recovery) AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
        else repository.resetPassword(password)
    }
    fun signOut() = perform(AuthOperation.SIGN_OUT) {
        repository.signOut().also { if (it == AuthResult.SignedOut) mutablePendingEmail.value = "" }
    }

    private fun perform(operation: AuthOperation, action: suspend () -> AuthResult) {
        if (mutableState.value is AuthUiState.Loading) return
        val previous = mutableState.value
        mutableState.value = AuthUiState.Loading(operation)
        viewModelScope.launch {
            try {
                mutableState.value = when (val result = action()) {
                    AuthResult.Ready -> AuthUiState.Idle
                    AuthResult.SignedOut -> AuthUiState.SignedOut
                    AuthResult.AwaitingVerification -> AuthUiState.PendingEmailVerification
                    AuthResult.RecoveryEmailRequested -> AuthUiState.RecoveryEmailRequested
                    AuthResult.ResettingPassword -> AuthUiState.ResettingPassword
                    AuthResult.VerificationEmailRequested -> AuthUiState.VerificationEmailRequested
                    AuthResult.PasswordUpdated -> AuthUiState.PasswordUpdated
                    is AuthResult.Authenticated -> AuthUiState.Authenticated(result.userId)
                    is AuthResult.Failure -> AuthUiState.Error(result.reason)
                }
            } catch (cancelled: CancellationException) {
                mutableState.value = previous
                throw cancelled
            } catch (_: IOException) {
                mutableState.value = AuthUiState.Error(AuthFailure.NETWORK)
            } catch (_: Exception) {
                mutableState.value = AuthUiState.Error(AuthFailure.UNKNOWN)
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(AuthDependencies.repository(context.applicationContext)) as T
            }
        }
    }
}
