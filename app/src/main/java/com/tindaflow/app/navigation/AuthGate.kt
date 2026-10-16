package com.tindaflow.app.navigation

import com.tindaflow.app.auth.AuthSession

/** Live session routing contract for M2.2b. Never sends a real account to DemoStore. */
fun authLandingRoute(session: AuthSession): Route = when (session) {
    AuthSession.SignedOut, AuthSession.Restoring, is AuthSession.Unavailable -> Route.LOGIN
    AuthSession.Unverified -> Route.VERIFY_EMAIL
    AuthSession.Recovery -> Route.RESET_PASSWORD
    is AuthSession.Verified -> Route.ONBOARDING // Workspace membership retrieval is M2.3.
}

/** M2.1 previews remain public; future live-route decisions must use this gate. */
fun mayEnterLiveRoute(route: Route, session: AuthSession): Boolean = when (route) {
    Route.RESET_PASSWORD -> session == AuthSession.Recovery
    Route.ONBOARDING, Route.SWITCHER, Route.PROFILE -> session is AuthSession.Verified
    Route.DEMO -> false
    else -> true
}
