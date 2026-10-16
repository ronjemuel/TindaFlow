package com.tindaflow.app.navigation

enum class Route {
    WELCOME, LOGIN, REGISTER, VERIFY_EMAIL, FORGOT_PASSWORD, RESET_PASSWORD,
    ONBOARDING, SWITCHER, PROFILE, LANGUAGE, DEMO
}

/** Only a future trusted auth adapter may supply an authenticated session. */
sealed interface SessionState {
    data object SignedOut : SessionState
    data object Demo : SessionState
    data class Authenticated(val emailVerified: Boolean, val hasWorkspace: Boolean) : SessionState
}

fun landingRoute(session: SessionState): Route = when (session) {
    SessionState.SignedOut -> Route.WELCOME
    SessionState.Demo -> Route.DEMO
    is SessionState.Authenticated -> when {
        !session.emailVerified -> Route.VERIFY_EMAIL
        !session.hasWorkspace -> Route.ONBOARDING
        else -> Route.SWITCHER
    }
}

/** M2.1 previews contain no live data. Demo content requires explicit demo entry. */
fun allowedRoute(route: Route, session: SessionState): Route =
    if (route == Route.DEMO && session != SessionState.Demo) landingRoute(session) else route

data class NavigationState(val stack: List<Route> = listOf(Route.WELCOME)) {
    val current: Route get() = stack.last()
    fun open(route: Route, session: SessionState): NavigationState {
        val target = allowedRoute(route, session)
        return if (target == current) this else copy(stack = stack + target)
    }
    fun back(): NavigationState = if (stack.size > 1) copy(stack = stack.dropLast(1)) else this
}
