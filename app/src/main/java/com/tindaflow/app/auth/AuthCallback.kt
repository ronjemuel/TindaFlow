package com.tindaflow.app.auth

import java.net.URI
import java.net.URLDecoder

/** Never log or stringify a callback. Only PKCE codes at the exact agreed URI are accepted. */
class AuthCallback private constructor(val code: String?, val error: AuthFailure?) {
    override fun toString() = "AuthCallback([redacted])"
    companion object {
        fun parse(value: String): AuthCallback? {
            if (value.length > 8192 || !SupabaseConfig.isCallbackOrigin(value)) return null
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            val params = runCatching {
                uri.rawQuery.orEmpty().split('&').filter(String::isNotEmpty).map {
                    val pair = it.split('=', limit = 2)
                    URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
                }
            }.getOrNull() ?: return null
            if (params.map { it.first }.distinct().size != params.size) return null
            val fields = params.toMap()
            if (fields.keys.any { it !in setOf("code", "error", "error_code", "error_description") }) return null
            if (fields.containsKey("error") || fields.containsKey("error_code"))
                return AuthCallback(null, AuthFailure.INVALID_RECOVERY)
            val code = fields["code"] ?: return null
            if (code.isBlank() || code.length > 4096 || code.any { it.isWhitespace() || it.isISOControl() }) return null
            return AuthCallback(code, null)
        }
    }
}

enum class PendingAuthFlow { CONFIRMATION, RECOVERY }

/** Bound to the encrypted verifier, not to untrusted URL parameters. */
data class PendingAuthContext(val flow: PendingAuthFlow, val createdAtMillis: Long) {
    fun isFresh(now: Long): Boolean = now >= createdAtMillis && now - createdAtMillis <= 60 * 60 * 1000L
}
