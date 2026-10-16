package com.tindaflow.app.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SupabaseConfigTest {
    private val key = "sb_publishable_unit_test_not_real"

    @Test fun emptyAndPartialConfigurationStayMissing() {
        assertEquals(AuthConfiguration.MISSING, SupabaseConfig("", "").status)
        assertEquals(AuthConfiguration.MISSING, SupabaseConfig("https://test.supabase.co", "").status)
    }

    @Test fun onlyHttpsOriginsAndPublishableKeysAreAccepted() {
        assertEquals(AuthConfiguration.READY, SupabaseConfig("https://test.supabase.co", key).status)
        listOf("http://test.supabase.co", "https://person:pass@test.supabase.co", "https://test.supabase.co/auth/v1",
            "https://test.supabase.co?key=x", "https://test.supabase.co#x", "https://test.supabase.co:443", "garbage").forEach {
            assertEquals(AuthConfiguration.INVALID, SupabaseConfig(it, key).status)
        }
        listOf("sb_secret_not_real", "eyJ.not.a.key", "sb_publishable_", "sb_publishable_with\nnewline").forEach {
            assertEquals(AuthConfiguration.INVALID, SupabaseConfig("https://test.supabase.co", it).status)
        }
    }

    @Test fun diagnosticStringDoesNotExposeConfiguration() {
        val config = SupabaseConfig("https://test.supabase.co", key)
        assertFalse(config.toString().contains(key))
        assertFalse(config.toString().contains(config.url))
    }

    @Test fun callbackShapeIsExactAndNeverMeansRecoveryWasVerified() {
        assertTrue(SupabaseConfig.isCallbackOrigin("tindaflow://auth/callback?code=synthetic"))
        listOf("tindaflow://auth", "tindaflow://auth/callback/extra", "tindaflow://evil/callback",
            "https://auth/callback", "tindaflow://person@auth/callback", "tindaflow://auth:123/callback",
            "tindaflow://auth/callback#access_token=synthetic").forEach {
            assertFalse(SupabaseConfig.isCallbackOrigin(it))
        }
    }

    @Test fun configuredFoundationStillCannotAuthenticateOrRestore() = runBlocking {
        val repository = SupabaseAuthRepository(SupabaseConfig("https://test.supabase.co", key))
        val blocked = AuthResult.Failure(AuthFailure.INTEGRATION_PENDING)
        assertEquals(blocked, repository.checkSetup())
        assertEquals(blocked, repository.restoreSession())
        assertEquals(blocked, repository.signIn("test@example.com", "synthetic"))
        assertEquals(blocked, repository.register("Test", "test@example.com", "synthetic"))
        assertEquals(blocked, repository.resendVerification("test@example.com"))
        assertEquals(blocked, repository.requestRecovery("test@example.com"))
        assertEquals(blocked, repository.signOut())
        assertEquals(AuthResult.Failure(AuthFailure.INVALID_RECOVERY), repository.resetPassword("synthetic"))
        assertEquals(AuthSession.SignedOut, repository.session.value)
    }

    @Test fun missingOrInvalidConfigurationCannotBecomeSuccess() = runBlocking {
        assertEquals(AuthResult.Failure(AuthFailure.CONFIGURATION_MISSING),
            SupabaseAuthRepository(SupabaseConfig("", "")).checkSetup())
        assertEquals(AuthResult.Failure(AuthFailure.CONFIGURATION_INVALID),
            SupabaseAuthRepository(SupabaseConfig("http://test.supabase.co", key)).checkSetup())
    }
}
