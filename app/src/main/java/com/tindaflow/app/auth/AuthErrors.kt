package com.tindaflow.app.auth

import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.security.GeneralSecurityException
import kotlinx.serialization.SerializationException

internal class AuthStorageException : Exception("Secure authentication storage is unavailable.")

/** Never forward exception messages: SDK exceptions can embed headers and tokens. */
fun authFailureForThrowable(error: Exception): AuthFailure = when (error) {
    is AuthStorageException -> AuthFailure.STORAGE
    is AuthWeakPasswordException -> AuthFailure.WEAK_PASSWORD
    is AuthRestException -> authFailureForCode(error.error).let {
        if (it == AuthFailure.UNKNOWN && error.statusCode >= 500) AuthFailure.SERVER else it
    }
    is RestException -> when {
        error.statusCode == 429 -> AuthFailure.RATE_LIMITED
        error.statusCode == 401 -> AuthFailure.SESSION_EXPIRED
        error.statusCode >= 500 -> AuthFailure.SERVER
        else -> AuthFailure.UNKNOWN
    }
    is HttpRequestException, is HttpRequestTimeoutException -> AuthFailure.NETWORK
    is GeneralSecurityException, is SerializationException -> AuthFailure.STORAGE
    is IOException -> AuthFailure.NETWORK
    else -> AuthFailure.UNKNOWN
}
