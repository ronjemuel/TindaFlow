package com.tindaflow.app.auth

import java.net.URI

enum class AuthConfiguration { MISSING, INVALID, READY }

/** Public client configuration only. Never include it in logs or UI diagnostics. */
class SupabaseConfig(val url: String, val publishableKey: String) {
    val status: AuthConfiguration
        get() {
            if (url.isBlank() || publishableKey.isBlank()) return AuthConfiguration.MISSING
            val origin = runCatching { URI(url) }.getOrNull() ?: return AuthConfiguration.INVALID
            return if (origin.scheme == "https" && !origin.host.isNullOrBlank() &&
                origin.userInfo == null && origin.port == -1 && origin.rawQuery == null &&
                origin.rawFragment == null && origin.path.orEmpty() in listOf("", "/") &&
                Regex("sb_publishable_[A-Za-z0-9_-]+").matches(publishableKey)) {
                AuthConfiguration.READY
            } else AuthConfiguration.INVALID
        }

    override fun toString() = "SupabaseConfig([redacted])"

    companion object {
        const val CALLBACK_URL = "tindaflow://auth/callback"
        /** URL shape validation is not proof of a genuine recovery context. */
        fun isCallbackOrigin(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            return uri.scheme == "tindaflow" && uri.host == "auth" && uri.path == "/callback" &&
                uri.userInfo == null && uri.port == -1 && uri.fragment == null
        }
    }
}
