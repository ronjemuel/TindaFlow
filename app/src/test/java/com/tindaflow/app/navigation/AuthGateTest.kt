package com.tindaflow.app.navigation

import com.tindaflow.app.auth.AuthSession
import org.junit.Assert.*
import org.junit.Test

class AuthGateTest {
    @Test fun untrustedSessionCannotEnterLiveRoutes() {
        listOf(AuthSession.SignedOut, AuthSession.Restoring, AuthSession.Unverified,
            AuthSession.Unavailable(com.tindaflow.app.auth.AuthFailure.NETWORK)).forEach { session ->
            listOf(Route.PROFILE, Route.SWITCHER, Route.ONBOARDING, Route.RESET_PASSWORD, Route.DEMO).forEach {
                assertFalse(mayEnterLiveRoute(it, session))
            }
        }
    }

    @Test fun verifiedUserIsNotAutomaticallyAuthorizedForPasswordRecoveryOrDemo() {
        val session = AuthSession.Verified("test-id")
        assertEquals(Route.ONBOARDING, authLandingRoute(session))
        assertTrue(mayEnterLiveRoute(Route.PROFILE, session))
        assertFalse(mayEnterLiveRoute(Route.RESET_PASSWORD, session))
        assertFalse(mayEnterLiveRoute(Route.DEMO, session))
    }

    @Test fun recoveryAndVerificationHaveDistinctDestinations() {
        assertEquals(Route.LOGIN, authLandingRoute(AuthSession.SignedOut))
        assertEquals(Route.LOGIN, authLandingRoute(AuthSession.Restoring))
        assertEquals(Route.VERIFY_EMAIL, authLandingRoute(AuthSession.Unverified))
        assertEquals(Route.RESET_PASSWORD, authLandingRoute(AuthSession.Recovery))
        assertTrue(mayEnterLiveRoute(Route.RESET_PASSWORD, AuthSession.Recovery))
        assertFalse(mayEnterLiveRoute(Route.PROFILE, AuthSession.Recovery))
    }
}
