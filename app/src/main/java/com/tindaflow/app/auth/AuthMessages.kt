package com.tindaflow.app.auth

import androidx.annotation.StringRes
import com.tindaflow.app.R

@StringRes
fun AuthFailure.messageResource(): Int = when (this) {
    AuthFailure.CONFIGURATION_MISSING -> R.string.auth_configuration_missing
    AuthFailure.CONFIGURATION_INVALID -> R.string.auth_configuration_invalid
    AuthFailure.INTEGRATION_PENDING -> R.string.auth_integration_pending
    AuthFailure.INVALID_CREDENTIALS -> R.string.auth_invalid_credentials
    AuthFailure.EMAIL_UNVERIFIED -> R.string.auth_email_unverified
    AuthFailure.NETWORK -> R.string.auth_network_error
    AuthFailure.RATE_LIMITED -> R.string.auth_rate_limited
    AuthFailure.INVALID_RECOVERY -> R.string.auth_invalid_recovery
    AuthFailure.SESSION_EXPIRED -> R.string.auth_session_expired
    AuthFailure.INVALID_INPUT -> R.string.check_input
    AuthFailure.WEAK_PASSWORD -> R.string.auth_password_rejected
    AuthFailure.ACCOUNT_UNAVAILABLE -> R.string.auth_account_unavailable
    AuthFailure.STORAGE -> R.string.auth_storage_error
    AuthFailure.WORKSPACE_PENDING -> R.string.auth_workspace_pending
    AuthFailure.SERVER, AuthFailure.UNKNOWN -> R.string.auth_unavailable
}
