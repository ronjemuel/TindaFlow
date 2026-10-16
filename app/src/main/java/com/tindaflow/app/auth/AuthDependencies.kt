package com.tindaflow.app.auth

import android.content.Context
import com.tindaflow.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-scoped SDK/repository; holds application context only. */
object AuthDependencies {
    @Volatile private var instance: AuthRepository? = null
    fun repository(context: Context): AuthRepository = instance ?: synchronized(this) {
        instance ?: run {
            val config = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            val backend: (() -> AuthRepository)? = if (config.status == AuthConfiguration.READY) {
                { SdkAuthBackend(context.applicationContext, config, CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
            } else null
            SupabaseAuthRepository(config, backend).also { instance = it }
        }
    }
}
