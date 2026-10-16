package com.tindaflow.app.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    @Test fun demoRequiresExplicitEntryAndIsNeverAuthenticatedLanding() {
        assertEquals(Route.WELCOME, allowedRoute(Route.DEMO, SessionState.SignedOut))
        assertEquals(Route.DEMO, allowedRoute(Route.DEMO, SessionState.Demo))
        assertEquals(Route.SWITCHER, allowedRoute(Route.DEMO, SessionState.Authenticated(true, true)))
    }

    @Test fun futureSessionGateRequiresVerificationBeforeWorkspaceSelection() {
        assertEquals(Route.WELCOME, landingRoute(SessionState.SignedOut))
        assertEquals(Route.VERIFY_EMAIL, landingRoute(SessionState.Authenticated(false, true)))
        assertEquals(Route.ONBOARDING, landingRoute(SessionState.Authenticated(true, false)))
        assertEquals(Route.SWITCHER, landingRoute(SessionState.Authenticated(true, true)))
    }

    @Test fun recoveryBackReturnsToLoginWithoutChangingSession() {
        val session = SessionState.SignedOut
        val login = NavigationState().open(Route.LOGIN, session)
        val recovery = login.open(Route.FORGOT_PASSWORD, session)
        assertEquals(Route.FORGOT_PASSWORD, recovery.current)
        assertEquals(login, recovery.back())
        assertEquals(Route.WELCOME, recovery.back().back().current)
        assertEquals(NavigationState(), NavigationState().back())
        assertEquals(login, login.open(Route.LOGIN, session))
    }

    @Test fun previewRoutesDoNotUnlockDemoOrCreateASession() {
        val session = SessionState.SignedOut
        val preview = NavigationState().open(Route.REGISTER, session)
            .open(Route.VERIFY_EMAIL, session).open(Route.ONBOARDING, session)
        assertEquals(Route.ONBOARDING, preview.current)
        assertEquals(Route.WELCOME, preview.open(Route.DEMO, session).current)
        assertEquals(Route.WELCOME, landingRoute(session))
    }

    @Test fun demoProfileAndBusinessSelectionReturnToDemo() {
        val demo = NavigationState(listOf(Route.DEMO))
        val profile = demo.open(Route.PROFILE, SessionState.Demo)
        val switcher = profile.open(Route.SWITCHER, SessionState.Demo)
        assertEquals(profile, switcher.back())
        assertEquals(demo, switcher.back().back())
    }
}
