package com.tindaflow.app.auth

import org.junit.Assert.*
import org.junit.Test

class AuthCallbackTest {
    @Test fun pkceCodeAtExactCallbackIsAcceptedWithoutExposingIt() {
        val callback = AuthCallback.parse("tindaflow://auth/callback?code=synthetic-code")!!
        assertEquals("synthetic-code", callback.code)
        assertNull(callback.error)
        assertFalse(callback.toString().contains("synthetic-code"))
    }

    @Test fun forgedRecoveryTypesTokensAndAmbiguousCodesAreRejected() {
        listOf(
            "tindaflow://auth/callback?code=a&type=recovery",
            "tindaflow://auth/callback#access_token=synthetic",
            "tindaflow://auth/callback?access_token=synthetic",
            "tindaflow://auth/callback?code=a&code=b",
            "tindaflow://auth/callback?code=a&%63ode=b",
            "tindaflow://auth/callback?code=",
            "tindaflow://auth/callback?code=%0A",
            "tindaflow://auth/callback/extra?code=a",
            "tindaflow://evil/callback?code=a",
            "https://auth/callback?code=a"
        ).forEach { assertNull(it, AuthCallback.parse(it)) }
    }

    @Test fun providerErrorsNeverBecomeRecoveryOrExposeDescriptions() {
        val result = AuthCallback.parse("tindaflow://auth/callback?error=access_denied&error_description=private&error_code=otp_expired")!!
        assertNull(result.code)
        assertEquals(AuthFailure.INVALID_RECOVERY, result.error)
        assertFalse(result.toString().contains("private"))
    }

    @Test fun pendingFlowsAreBoundedAndClockRollbackFailsClosed() {
        val pending = PendingAuthContext(PendingAuthFlow.RECOVERY, 1000)
        assertTrue(pending.isFresh(2000))
        assertFalse(pending.isFresh(999))
        assertFalse(pending.isFresh(1000 + 60 * 60 * 1000L + 1))
    }

    @Test fun pkceChallengeMatchesRfc7636AndVerifiersAreRandom() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            AuthPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        val first = AuthPkce.verifier()
        assertTrue(first.length in 43..128)
        assertTrue(first.matches(Regex("[A-Za-z0-9_-]+")))
        assertNotEquals(first, AuthPkce.verifier())
    }
}
