package com.tindaflow.app.auth

enum class InputError { REQUIRED, EMAIL, PASSWORD_LENGTH, PASSWORD_MISMATCH, TOO_LONG }

object InputValidation {
    fun email(value: String): InputError? = when {
        value.isBlank() -> InputError.REQUIRED
        value.trim().length > 254 -> InputError.TOO_LONG
        !Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$").matches(value.trim()) -> InputError.EMAIL
        else -> null
    }
    // Initial UI policy; the configured provider's policy remains authoritative in M2.2.
    fun password(value: String): InputError? = when {
        value.isEmpty() -> InputError.REQUIRED
        value.length < 8 -> InputError.PASSWORD_LENGTH
        value.length > 128 -> InputError.TOO_LONG
        else -> null
    }
    fun loginPassword(value: String): InputError? = if (value.isEmpty()) InputError.REQUIRED else null
    fun confirmation(password: String, confirmation: String): InputError? = when {
        confirmation.isEmpty() -> InputError.REQUIRED
        password != confirmation -> InputError.PASSWORD_MISMATCH
        else -> null
    }
    fun businessName(value: String): InputError? = when {
        value.isBlank() -> InputError.REQUIRED
        value.trim().length > 100 -> InputError.TOO_LONG
        else -> null
    }
}

enum class BusinessType { RETAIL, SERVICES, BOTH }

/** No credentials are accepted, saved, logged, or transmitted by this adapter. */
interface AccountAvailability {
    suspend fun check(): AccountActionState
}
enum class AccountActionState { IDLE, LOADING, SETUP_REQUIRED }
object UnconfiguredAccountAvailability : AccountAvailability {
    override suspend fun check() = AccountActionState.SETUP_REQUIRED
}
