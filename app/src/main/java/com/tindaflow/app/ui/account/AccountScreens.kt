package com.tindaflow.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.tindaflow.app.R
import com.tindaflow.app.auth.*
import com.tindaflow.app.data.DemoStore
import com.tindaflow.app.navigation.Route
import com.tindaflow.app.ui.TindaColors
import com.tindaflow.app.ui.components.BrandMark
import com.tindaflow.app.ui.components.SurfacePanel

val LocalAuthModel = staticCompositionLocalOf<AuthViewModel> { error("Authentication host required.") }
val LocalDemoMode = staticCompositionLocalOf { false }

@Composable
fun AccountPage(title: Int, back: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val action by LocalAuthModel.current.state.collectAsState()
    Column(Modifier.fillMaxSize().background(TindaColors.Canvas).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (back != null) IconButton(onClick = back, enabled = action !is AuthUiState.Loading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            BrandMark()
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = TindaColors.Deep)
        }
        Text(stringResource(if (LocalDemoMode.current) R.string.m2_demo_banner else R.string.m2_foundation),
            style = MaterialTheme.typography.labelMedium, color = TindaColors.Forest)
        Text(stringResource(title), style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PrimaryAction(label: Int, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = !busy && enabled, shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), contentPadding = PaddingValues(16.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(stringResource(if (busy) R.string.m2_loading else label))
    }
}

@Composable
private fun Link(label: Int, onClick: () -> Unit) {
    val action by LocalAuthModel.current.state.collectAsState()
    TextButton(onClick, enabled = action !is AuthUiState.Loading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(label)) }
}

@Composable
private fun ErrorText(error: InputError?) {
    if (error != null) Text(stringResource(when (error) {
        InputError.REQUIRED -> R.string.m2_required
        InputError.EMAIL -> R.string.m2_invalid_email
        InputError.PASSWORD_LENGTH -> R.string.m2_short_password
        InputError.PASSWORD_MISMATCH -> R.string.m2_password_mismatch
        InputError.TOO_LONG -> R.string.m2_too_long
    }))
}

@Composable
private fun Input(value: String, onChange: (String) -> Unit, label: Int,
                  error: InputError? = null, password: Boolean = false,
                  email: Boolean = false, last: Boolean = false, onDone: () -> Unit = {}, enabled: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(stringResource(label)) }, singleLine = true, isError = error != null,
        shape = RoundedCornerShape(14.dp), supportingText = { ErrorText(error) },
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) ({
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    stringResource(if (visible) R.string.m2_hide_password else R.string.m2_show_password))
            }
        }) else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else if (email) KeyboardType.Email else KeyboardType.Text,
            autoCorrectEnabled = !password && !email,
            imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
            onDone = { focus.clearFocus(); onDone() }))
}

/** Business creation is explicitly deferred to M2.3. */
@Composable
private fun rememberAccountAction(): Pair<AuthUiState, () -> Unit> {
    var state by remember { mutableStateOf<AuthUiState>(AuthUiState.Idle) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val run: () -> Unit = {
        if (state !is AuthUiState.Loading) {
            keyboard?.hide()
            focus.clearFocus()
            state = AuthUiState.Error(AuthFailure.WORKSPACE_PENDING)
        }
    }
    return state to run
}

@Composable
private fun AvailabilityNotice(state: AuthUiState) {
    if (state is AuthUiState.Error) SurfacePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.auth_action_error), fontWeight = FontWeight.SemiBold, color = TindaColors.Amber)
            Text(stringResource(state.reason.messageResource()), color = TindaColors.Muted)
        }
    }
    val message = when (state) {
        AuthUiState.PendingEmailVerification, AuthUiState.VerificationEmailRequested -> R.string.auth_verification_requested
        AuthUiState.RecoveryEmailRequested -> R.string.auth_recovery_requested
        AuthUiState.PasswordUpdated -> R.string.auth_password_updated
        else -> null
    }
    if (message != null) Text(stringResource(message), color = TindaColors.Forest,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
fun WelcomeScreen(open: (Route) -> Unit, enterDemo: () -> Unit) {
    AccountPage(R.string.m2_welcome, null) {
        Text(stringResource(R.string.m2_welcome_note), style = MaterialTheme.typography.bodyLarge, color = TindaColors.Muted)
        PrimaryAction(R.string.m2_login) { open(Route.LOGIN) }
        Link(R.string.m2_register) { open(Route.REGISTER) }
        SurfacePanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.m2_setup), color = TindaColors.Muted)
            Link(R.string.m2_demo, enterDemo)
        }
        Link(R.string.m2_language) { open(Route.LANGUAGE) }
        var previews by rememberSaveable { mutableStateOf(false) }
        Link(R.string.m2_preview) { previews = !previews }
        if (previews) {
            Link(R.string.m2_view_verify) { open(Route.VERIFY_EMAIL) }
            Link(R.string.m2_view_reset) { open(Route.RESET_PASSWORD) }
            Link(R.string.m2_view_business) { open(Route.ONBOARDING) }
            Link(R.string.m2_switcher) { open(Route.SWITCHER) }
            Link(R.string.m2_profile) { open(Route.PROFILE) }
        }
    }
}

@Composable
fun AuthFormScreen(route: Route, open: (Route) -> Unit, back: () -> Unit) {
    val model = LocalAuthModel.current
    val action by model.state.collectAsState()
    val session by model.session.collectAsState()
    val register = route == Route.REGISTER
    val reset = route == Route.RESET_PASSWORD
    val forgot = route == Route.FORGOT_PASSWORD
    val title = when (route) {
        Route.REGISTER -> R.string.m2_register
        Route.RESET_PASSWORD -> R.string.m2_reset
        Route.FORGOT_PASSWORD -> R.string.m2_recovery
        else -> R.string.m2_login
    }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately excluded from saved instance state and preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val nameError = if (register) InputValidation.businessName(name) else null
    val emailError = if (reset) null else InputValidation.email(email)
    val passwordError = if (forgot) null else if (register || reset) InputValidation.password(password) else InputValidation.loginPassword(password)
    val confirmError = if (register || reset) InputValidation.confirmation(password, confirmation) else null
    val submit: () -> Unit = submit@{
        if (action is AuthUiState.Loading) return@submit
        submitted = true
        if (emailError == null && passwordError == null && confirmError == null && nameError == null) {
            keyboard?.hide(); focus.clearFocus()
            when (route) {
                Route.REGISTER -> model.register(name, email, password)
                Route.FORGOT_PASSWORD -> model.requestRecovery(email)
                Route.RESET_PASSWORD -> model.resetPassword(password)
                else -> model.signIn(email, password)
            }
            password = ""; confirmation = ""; submitted = false
        }
    }
    AccountPage(title, back) {
        Text(stringResource(when {
            reset -> R.string.m2_reset_note
            forgot -> R.string.m2_recovery_note
            else -> R.string.m2_auth_note
        }), color = TindaColors.Muted)
        if (register) Input(name, { name = it }, R.string.auth_display_name,
            if (submitted) nameError else null, enabled = action !is AuthUiState.Loading)
        if (!reset) Input(email, { email = it }, R.string.m2_email,
            if (submitted) emailError else null, email = true, last = forgot, onDone = submit, enabled = action !is AuthUiState.Loading)
        if (!forgot) Input(password, { password = it }, if (reset) R.string.m2_new_password else R.string.m2_password,
            if (submitted) passwordError else null, password = true, last = !register && !reset, onDone = submit, enabled = action !is AuthUiState.Loading)
        if (register || reset) {
            Text(stringResource(R.string.m2_password_hint), style = MaterialTheme.typography.bodyMedium, color = TindaColors.Muted)
            Input(confirmation, { confirmation = it }, R.string.m2_confirm_password,
                if (submitted) confirmError else null, password = true, last = true, onDone = submit, enabled = action !is AuthUiState.Loading)
        }
        AvailabilityNotice(action)
        PrimaryAction(if (forgot) R.string.m2_send_link else title, action is AuthUiState.Loading,
            enabled = !reset || session == AuthSession.Recovery, onClick = submit)
        if (action is AuthUiState.Error && (action as AuthUiState.Error).reason in
            setOf(AuthFailure.NETWORK, AuthFailure.SESSION_EXPIRED, AuthFailure.STORAGE))
            Link(R.string.auth_retry_session) { model.restoreSession() }
        if (route == Route.LOGIN) {
            Link(R.string.m2_forgot) { open(Route.FORGOT_PASSWORD) }
            Link(R.string.m2_register) { open(Route.REGISTER) }
        }
        if (register) Link(R.string.m2_view_verify) { open(Route.VERIFY_EMAIL) }
        if (forgot) Link(R.string.m2_view_reset) { open(Route.RESET_PASSWORD) }
    }
}

@Composable
fun VerifyEmailScreen(open: (Route) -> Unit, back: () -> Unit) {
    val model = LocalAuthModel.current
    val action by model.state.collectAsState()
    var email by rememberSaveable { mutableStateOf(model.pendingEmail.value) }
    var submitted by remember { mutableStateOf(false) }
    AccountPage(R.string.m2_check_inbox, back) {
        Text(stringResource(R.string.m2_verify_note), color = TindaColors.Muted)
        Input(email, { email = it }, R.string.m2_email, if (submitted) InputValidation.email(email) else null, email = true)
        AvailabilityNotice(action)
        PrimaryAction(R.string.m2_check_status, action is AuthUiState.Loading, onClick = model::checkVerification)
        Link(R.string.m2_resend) {
            submitted = true
            if (InputValidation.email(email) == null) model.resendVerification(email)
        }
        Link(R.string.m2_return_login) { open(Route.LOGIN) }
    }
}

@Composable
fun OnboardingScreen(open: (Route) -> Unit, back: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(BusinessType.BOTH) }
    var submitted by remember { mutableStateOf(false) }
    val (action, run) = rememberAccountAction()
    AccountPage(R.string.m2_onboarding, back) {
        Text(stringResource(R.string.m2_onboarding_note), color = TindaColors.Muted)
        Input(name, { name = it }, R.string.m2_business_name, if (submitted) InputValidation.businessName(name) else null)
        Text(stringResource(R.string.m2_business_type), fontWeight = FontWeight.SemiBold)
        Column(Modifier.semantics { isTraversalGroup = true }) {
            BusinessType.entries.forEach { candidate ->
                val label = when (candidate) {
                    BusinessType.RETAIL -> R.string.m2_retail
                    BusinessType.SERVICES -> R.string.m2_services
                    BusinessType.BOTH -> R.string.m2_both
                }
                TypeOption(stringResource(label), candidate == type) { type = candidate }
            }
        }
        OutlinedTextField(details, { details = it.take(500) }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.m2_details)) }, minLines = 2, maxLines = 5,
            shape = RoundedCornerShape(14.dp))
        Text(stringResource(R.string.m2_defaults), color = TindaColors.Forest)
        AvailabilityNotice(action)
        PrimaryAction(R.string.m2_create_business, action is AuthUiState.Loading) {
            submitted = true
            if (InputValidation.businessName(name) == null) run()
        }
        Link(R.string.m2_switcher) { open(Route.SWITCHER) }
        Link(R.string.m2_profile) { open(Route.PROFILE) }
        AvailabilityNotice(LocalAuthModel.current.state.collectAsState().value)
    }
}

@Composable
private fun TypeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        .semantics { this.selected = selected; role = Role.RadioButton },
        shape = RoundedCornerShape(14.dp), color = if (selected) TindaColors.LightGreen else MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = null)
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
    }
}

@Composable
fun BusinessSwitcherScreen(store: DemoStore, demo: Boolean, open: (Route) -> Unit, back: () -> Unit) {
    AccountPage(R.string.m2_switcher, back) {
        SurfacePanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.m2_no_workspaces), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.m2_workspace_note), color = TindaColors.Muted)
        }
        Link(R.string.m2_view_business) { open(Route.ONBOARDING) }
        if (demo) {
            Text(stringResource(R.string.m2_demo_workspaces), fontWeight = FontWeight.SemiBold)
            store.workspaces.forEachIndexed { index, workspace ->
                Surface(onClick = { store.switchBusiness(index) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(workspace.name, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.m2_sample_role), color = TindaColors.Muted)
                        if (store.activeIndex == index) Text(stringResource(R.string.m2_active_demo), color = TindaColors.Forest)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(demo: Boolean, open: (Route) -> Unit, leaveDemo: () -> Unit, back: () -> Unit) {
    val model = LocalAuthModel.current
    val action by model.state.collectAsState()
    val session by model.session.collectAsState()
    val authenticated = !demo && session is AuthSession.Verified
    AccountPage(R.string.m2_profile, back) {
        SurfacePanel(Modifier.fillMaxWidth()) {
            Text(stringResource(if (authenticated) R.string.auth_signed_in else R.string.m2_no_account), fontWeight = FontWeight.SemiBold)
            Text(stringResource(if (authenticated) R.string.auth_profile_note else R.string.m2_profile_note), color = TindaColors.Muted)
        }
        Link(R.string.m2_switcher) { open(Route.SWITCHER) }
        Link(R.string.m2_language) { open(Route.LANGUAGE) }
        Link(R.string.m2_view_business) { open(Route.ONBOARDING) }
        AvailabilityNotice(action)
        if (demo) PrimaryAction(R.string.m2_exit_demo, onClick = leaveDemo)
        else if (authenticated) {
            PrimaryAction(R.string.m2_sign_out, action is AuthUiState.Loading, onClick = model::signOut)
            Link(R.string.m2_demo) { open(Route.DEMO) }
        } else PrimaryAction(R.string.m2_login) { open(Route.LOGIN) }
    }
}

@Composable
fun LanguageScreen(language: String?, choose: (String) -> Unit, back: () -> Unit) {
    AccountPage(R.string.m2_language, back) {
        Text(stringResource(R.string.m2_language_note), color = TindaColors.Muted)
        TypeOption(stringResource(R.string.m2_english), language == "en") { choose("en") }
        TypeOption(stringResource(R.string.m2_filipino), language == "fil") { choose("fil") }
    }
}
