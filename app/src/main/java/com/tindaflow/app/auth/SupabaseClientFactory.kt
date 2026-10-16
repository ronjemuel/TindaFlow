package com.tindaflow.app.auth

import android.content.Context
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.ktor.client.engine.okhttp.OkHttp

/** One application-scoped client, initialized on first authentication operation. */
object SupabaseClientFactory {
    fun create(context: Context, configuration: SupabaseConfig): io.github.jan.supabase.SupabaseClient {
        require(configuration.status == AuthConfiguration.READY) { "Supabase client configuration is not ready." }
        val storage = KeystoreAuthStorage(context.applicationContext, configuration.url)
        return createSupabaseClient(configuration.url.trimEnd('/'), configuration.publishableKey) {
            defaultLogLevel = LogLevel.NONE
            httpEngine = OkHttp.create()
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "tindaflow"
                host = "auth"
                defaultRedirectUrl = SupabaseConfig.CALLBACK_URL
                sessionManager = storage
                codeVerifierCache = storage
                // Explicit loading lets the repository handle storage/network failures safely.
                autoLoadFromStorage = false
                autoSaveToStorage = true
            }
        }
    }
}
