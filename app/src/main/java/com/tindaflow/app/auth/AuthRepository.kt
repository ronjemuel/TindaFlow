package com.tindaflow.app.auth

import kotlinx.coroutines.flow.StateFlow

/** Token-free session summary; tokens and passwords must never enter Compose UI state. */
sealed interface AuthSession {
    data object SignedOut : AuthSession
    data object Restoring : AuthSession
    data class Verified(val userId: String) : AuthSession
    data object Unverified : AuthSession
    data object Recovery : AuthSession
    data class Unavailable(val reason: AuthFailure) : AuthSession
}

enum class AuthOperation { CHECK_SETUP, RESTORE, SIGN_IN, REGISTER, RESEND, RECOVER, RESET, SIGN_OUT, CALLBACK, VERIFY }
enum class AuthFailure {
    CONFIGURATION_MISSING, CONFIGURATION_INVALID, INTEGRATION_PENDING,
    INVALID_CREDENTIALS, EMAIL_UNVERIFIED, NETWORK, RATE_LIMITED,
    INVALID_RECOVERY, SESSION_EXPIRED, SERVER, UNKNOWN, INVALID_INPUT, WEAK_PASSWORD,
    ACCOUNT_UNAVAILABLE, STORAGE, WORKSPACE_PENDING
}

sealed interface AuthResult {
    data object Ready : AuthResult
    data object SignedOut : AuthResult
    data object AwaitingVerification : AuthResult
    data object RecoveryEmailRequested : AuthResult
    data object ResettingPassword : AuthResult
    data object VerificationEmailRequested : AuthResult
    data object PasswordUpdated : AuthResult
    data class Authenticated(val userId: String) : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Loading(val operation: AuthOperation) : AuthUiState
    data object SignedOut : AuthUiState
    data object PendingEmailVerification : AuthUiState
    data object RecoveryEmailRequested : AuthUiState
    data object ResettingPassword : AuthUiState
    data object VerificationEmailRequested : AuthUiState
    data object PasswordUpdated : AuthUiState
    data class Authenticated(val userId: String) : AuthUiState
    data class Error(val reason: AuthFailure) : AuthUiState
}

/** The UI never handles SDK sessions. Real journey implementations are M2.2b. */
interface AuthRepository {
    val session: StateFlow<AuthSession>
    suspend fun checkSetup(): AuthResult
    suspend fun restoreSession(): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun resendVerification(email: String): AuthResult
    suspend fun requestRecovery(email: String): AuthResult
    suspend fun resetPassword(password: String): AuthResult
    suspend fun signOut(): AuthResult
    suspend fun handleCallback(url: String): AuthResult = AuthResult.Failure(AuthFailure.INVALID_RECOVERY)
    suspend fun checkVerification(): AuthResult = restoreSession()
}

/** Map structured provider codes only, never server descriptions which can contain PII. */
fun authFailureForCode(code: String?): AuthFailure = when (code) {
    "invalid_credentials" -> AuthFailure.INVALID_CREDENTIALS
    "email_not_confirmed" -> AuthFailure.EMAIL_UNVERIFIED
    "over_request_rate_limit", "over_email_send_rate_limit" -> AuthFailure.RATE_LIMITED
    "otp_expired", "flow_state_expired", "flow_state_not_found", "bad_code_verifier" -> AuthFailure.INVALID_RECOVERY
    "session_expired", "refresh_token_not_found", "refresh_token_already_used" -> AuthFailure.SESSION_EXPIRED
    "weak_password", "same_password" -> AuthFailure.WEAK_PASSWORD
    "user_banned", "signup_disabled", "email_provider_disabled", "email_address_not_authorized", "captcha_failed" -> AuthFailure.ACCOUNT_UNAVAILABLE
    else -> AuthFailure.UNKNOWN
}
