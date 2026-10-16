package com.tindaflow.app.navigation

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.tindaflow.app.TindaApp
import com.tindaflow.app.data.DemoStore
import com.tindaflow.app.ui.account.*
import com.tindaflow.app.auth.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tindaflow.app.R

@Composable
fun TindaRoot(auth: AuthViewModel) {
    val authSession by auth.session.collectAsState()
    val action by auth.state.collectAsState()
    val store = remember { DemoStore() }
    val screenState = rememberSaveableStateHolder()
    var demo by rememberSaveable { mutableStateOf(false) }
    var routeNames by rememberSaveable { mutableStateOf(listOf(Route.WELCOME.name)) }
    // Preview preference only: no saved language or auth session is claimed in M2.1.
    var language by remember { mutableStateOf<String?>(null) }
    val session = if (demo) SessionState.Demo else SessionState.SignedOut
    val navigation = NavigationState(routeNames.map(Route::valueOf))
    val route = when {
        authSession == AuthSession.Recovery -> authLandingRoute(authSession)
        !demo && authSession is AuthSession.Verified && navigation.current in
            setOf(Route.LOGIN, Route.REGISTER, Route.WELCOME, Route.FORGOT_PASSWORD, Route.VERIFY_EMAIL, Route.RESET_PASSWORD) -> authLandingRoute(authSession)
        else -> allowedRoute(navigation.current, session)
    }
    val open: (Route) -> Unit = { target ->
        if (action !is AuthUiState.Loading) {
            auth.clearAction()
            if (target == Route.DEMO) { demo = true; routeNames = listOf(Route.DEMO.name) }
            else routeNames = navigation.open(target, session).stack.map(Route::name)
        }
    }
    val back: () -> Unit = {
        if (action !is AuthUiState.Loading) {
            if (authSession == AuthSession.Recovery) auth.signOut()
            else {
                auth.clearAction()
                routeNames = if (navigation.stack.size == 1 && navigation.current != Route.WELCOME)
                    listOf(if (authSession is AuthSession.Verified) Route.PROFILE.name else Route.WELCOME.name)
                else navigation.back().stack.map(Route::name)
            }
        }
    }
    LaunchedEffect(authSession) {
        when (authSession) {
            is AuthSession.Verified -> { demo = false; routeNames = listOf(Route.ONBOARDING.name) }
            AuthSession.Recovery -> { demo = false; routeNames = listOf(Route.RESET_PASSWORD.name) }
            is AuthSession.Unavailable -> { demo = false; routeNames = listOf(Route.LOGIN.name) }
            else -> Unit
        }
    }
    LaunchedEffect(action) {
        when (action) {
            AuthUiState.PendingEmailVerification -> { demo = false; routeNames = listOf(Route.VERIFY_EMAIL.name) }
            AuthUiState.SignedOut -> {
                Route.entries.filter { it != Route.DEMO }.forEach { screenState.removeState(it.name) }
                demo = false; routeNames = listOf(Route.LOGIN.name)
            }
            is AuthUiState.Error -> if ((action as AuthUiState.Error).reason == AuthFailure.EMAIL_UNVERIFIED) {
                demo = false; routeNames = listOf(Route.VERIFY_EMAIL.name)
            }
            else -> Unit
        }
    }
    val leaveDemo: () -> Unit = {
        store.resetCart()
        screenState.removeState(Route.DEMO.name)
        demo = false
        routeNames = listOf(Route.WELCOME.name)
    }
    BackHandler(enabled = (navigation.stack.size > 1 || authSession == AuthSession.Recovery || action is AuthUiState.Loading)
        && route != Route.DEMO, onBack = back)
    // At demo home, Back returns to Welcome; checkout's later BackHandler takes precedence.
    BackHandler(enabled = route == Route.DEMO, onBack = leaveDemo)

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedConfiguration = remember(configuration, language) {
        Configuration(configuration).apply { language?.let { setLocales(LocaleList.forLanguageTags(it)) } }
    }
    val localizedContext = remember(context, localizedConfiguration) { context.createConfigurationContext(localizedConfiguration) }
    CompositionLocalProvider(LocalContext provides localizedContext, LocalConfiguration provides localizedConfiguration,
        LocalAuthModel provides auth, LocalDemoMode provides demo) {
        if (!demo && authSession == AuthSession.Restoring) {
            AccountPage(R.string.auth_restoring, null) {
                CircularProgressIndicator()
                TextButton(onClick = { demo = true; routeNames = listOf(Route.DEMO.name) }) {
                    Text(stringResource(R.string.m2_demo))
                }
            }
            return@CompositionLocalProvider
        }
        screenState.SaveableStateProvider(route.name) {
            when (route) {
                Route.WELCOME -> WelcomeScreen(open) {
                    demo = true
                    routeNames = listOf(Route.DEMO.name)
                }
                Route.LOGIN, Route.REGISTER, Route.FORGOT_PASSWORD, Route.RESET_PASSWORD -> AuthFormScreen(route, open, back)
                Route.VERIFY_EMAIL -> VerifyEmailScreen(open, back)
                Route.ONBOARDING -> OnboardingScreen(open, back)
                Route.SWITCHER -> BusinessSwitcherScreen(store, demo, open, back)
                Route.PROFILE -> ProfileScreen(demo, open, leaveDemo, back)
                Route.LANGUAGE -> LanguageScreen(language ?: if (configuration.locales[0].language == "fil") "fil" else "en",
                    { language = it }, back)
                Route.DEMO -> TindaApp(store = store, onProfile = { open(Route.PROFILE) })
            }
        }
    }
}
