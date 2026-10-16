package com.tindaflow.app.auth

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Before fun setMain() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun resetMain() { Dispatchers.resetMain() }

    private class FakeRepository : AuthRepository {
        override val session = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
        var result: AuthResult = AuthResult.Failure(AuthFailure.CONFIGURATION_MISSING)
        var exception: Exception? = null
        var pending: CompletableDeferred<AuthResult>? = null
        var calls = 0
        var resetCalls = 0
        var callbackCalls = 0
        override suspend fun handleCallback(url: String): AuthResult {
            callbackCalls++
            session.value = AuthSession.Recovery
            return AuthResult.ResettingPassword
        }
        suspend fun respond(): AuthResult {
            calls++
            exception?.let { throw it }
            val response = pending?.await() ?: result
            when (response) {
                AuthResult.SignedOut -> session.value = AuthSession.SignedOut
                AuthResult.AwaitingVerification -> session.value = AuthSession.Unverified
                AuthResult.ResettingPassword -> session.value = AuthSession.Recovery
                is AuthResult.Authenticated -> session.value = AuthSession.Verified(response.userId)
                else -> Unit
            }
            return response
        }
        override suspend fun checkSetup() = respond()
        override suspend fun restoreSession() = respond()
        override suspend fun signIn(email: String, password: String) = respond()
        override suspend fun register(name: String, email: String, password: String) = respond()
        override suspend fun resendVerification(email: String) = respond()
        override suspend fun requestRecovery(email: String) = respond()
        override suspend fun resetPassword(password: String): AuthResult { resetCalls++; return respond() }
        override suspend fun signOut() = respond()
    }

    @Test fun setupTransitionsFromIdleThroughLoadingToMissingConfiguration() = runTest {
        val vm = AuthViewModel(FakeRepository())
        assertEquals(AuthUiState.Idle, vm.state.value)
        vm.checkSetup()
        assertEquals(AuthUiState.Loading(AuthOperation.CHECK_SETUP), vm.state.value)
        advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.CONFIGURATION_MISSING), vm.state.value)
    }

    @Test fun registrationWaitsForVerificationRatherThanAuthenticating() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.AwaitingVerification }
        val vm = AuthViewModel(repo)
        vm.register("Test", "test@example.com", "synthetic")
        advanceUntilIdle()
        assertEquals(AuthUiState.PendingEmailVerification, vm.state.value)
        assertEquals(AuthSession.Unverified, vm.session.value)
    }

    @Test fun duplicateSubmitsAreBlockedUntilFirstRequestCompletes() = runTest {
        val gate = CompletableDeferred<AuthResult>()
        val repo = FakeRepository().apply { pending = gate }
        val vm = AuthViewModel(repo)
        vm.signIn("test@example.com", "synthetic")
        vm.signIn("test@example.com", "synthetic")
        runCurrent()
        assertEquals(1, repo.calls)
        gate.complete(AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS))
        advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.INVALID_CREDENTIALS), vm.state.value)
        assertEquals(AuthSession.SignedOut, vm.session.value)
    }

    @Test fun offlineAndUnknownExceptionsProduceSanitizedFailures() = runTest {
        val repo = FakeRepository().apply { exception = IOException("private provider details") }
        val vm = AuthViewModel(repo)
        vm.signIn("test@example.com", "synthetic")
        advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.NETWORK), vm.state.value)
        repo.exception = IllegalStateException("private provider details")
        vm.checkSetup()
        advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.UNKNOWN), vm.state.value)
        assertFalse(vm.state.value.toString().contains("private provider details"))
    }

    @Test fun resetWithoutRecoveryContextNeverReachesRepository() = runTest {
        val repo = FakeRepository()
        val vm = AuthViewModel(repo)
        vm.resetPassword("synthetic")
        advanceUntilIdle()
        assertEquals(0, repo.resetCalls)
        assertEquals(AuthUiState.Error(AuthFailure.INVALID_RECOVERY), vm.state.value)
        repo.session.value = AuthSession.Verified("test-id")
        vm.resetPassword("synthetic")
        advanceUntilIdle()
        assertEquals(0, repo.resetCalls)
    }

    @Test fun trustedRecoveryContextAllowsResetContract() = runTest {
        val repo = FakeRepository().apply {
            session.value = AuthSession.Recovery
            result = AuthResult.SignedOut
        }
        val vm = AuthViewModel(repo)
        vm.resetPassword("synthetic")
        advanceUntilIdle()
        assertEquals(1, repo.resetCalls)
        assertEquals(AuthUiState.SignedOut, vm.state.value)
    }

    @Test fun restorationAndSignOutReflectRepositorySessionWithoutDemoData() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.Authenticated("test-id") }
        val vm = AuthViewModel(repo)
        vm.restoreSession()
        advanceUntilIdle()
        assertEquals(AuthUiState.Authenticated("test-id"), vm.state.value)
        assertEquals(AuthSession.Verified("test-id"), vm.session.value)
        repo.result = AuthResult.SignedOut
        vm.signOut()
        advanceUntilIdle()
        assertEquals(AuthUiState.SignedOut, vm.state.value)
        assertEquals(AuthSession.SignedOut, vm.session.value)
    }

    @Test fun expiredSessionCannotBeRestoredAsAuthenticated() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.Failure(AuthFailure.SESSION_EXPIRED) }
        val vm = AuthViewModel(repo)
        vm.restoreSession()
        advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.SESSION_EXPIRED), vm.state.value)
        assertEquals(AuthSession.SignedOut, vm.session.value)
    }

    @Test fun cancellationIsNotReportedAsAnAuthenticationError() = runTest {
        val vm = AuthViewModel(FakeRepository().apply { exception = CancellationException() })
        vm.checkSetup()
        advanceUntilIdle()
        assertEquals(AuthUiState.Idle, vm.state.value)
    }

    @Test fun clearingViewModelCancelsPendingWork() = runTest {
        val repo = FakeRepository().apply { pending = CompletableDeferred() }
        val vm = AuthViewModel(repo)
        val store = ViewModelStore().apply { put("auth", vm) }
        vm.checkSetup()
        runCurrent()
        store.clear()
        runCurrent()
        assertFalse(vm.state.value is AuthUiState.Loading)
        assertEquals(AuthSession.SignedOut, vm.session.value)
    }

    @Test fun recoveryEmailResultDoesNotAuthenticate() = runTest {
        val vm = AuthViewModel(FakeRepository().apply { result = AuthResult.RecoveryEmailRequested })
        vm.requestRecovery("test@example.com")
        advanceUntilIdle()
        assertEquals(AuthUiState.RecoveryEmailRequested, vm.state.value)
        assertEquals(AuthSession.SignedOut, vm.session.value)
    }

    @Test fun structuredErrorsHaveLocalizedResourceMappings() {
        assertEquals(AuthFailure.INVALID_CREDENTIALS, authFailureForCode("invalid_credentials"))
        assertEquals(AuthFailure.EMAIL_UNVERIFIED, authFailureForCode("email_not_confirmed"))
        assertEquals(AuthFailure.RATE_LIMITED, authFailureForCode("over_email_send_rate_limit"))
        assertEquals(AuthFailure.INVALID_RECOVERY, authFailureForCode("flow_state_expired"))
        assertEquals(AuthFailure.SESSION_EXPIRED, authFailureForCode("refresh_token_not_found"))
        assertEquals(AuthFailure.UNKNOWN, authFailureForCode("unrecognized provider data"))
        AuthFailure.entries.forEach { assertTrue(it.messageResource() != 0) }
    }

    @Test fun startupRestoresOnceAndDoesNotSubmitDuplicateRequests() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.Authenticated("test-id") }
        val vm = AuthViewModel(repo)
        val store = ViewModelStore().apply { put("auth", vm) }
        vm.start(); vm.start()
        advanceUntilIdle()
        assertEquals(1, repo.calls)
        assertEquals(AuthSession.Verified("test-id"), vm.session.value)
        store.clear()
    }

    @Test fun callbackWaitsForStartupRestorationInsteadOfBeingDropped() = runTest {
        val gate = CompletableDeferred<AuthResult>()
        val repo = FakeRepository().apply { pending = gate }
        val vm = AuthViewModel(repo)
        val store = ViewModelStore().apply { put("auth", vm) }
        vm.start()
        vm.handleCallback("tindaflow://auth/callback?code=synthetic")
        runCurrent()
        assertEquals(0, repo.callbackCalls)
        gate.complete(AuthResult.SignedOut)
        advanceUntilIdle()
        assertEquals(1, repo.callbackCalls)
        assertEquals(AuthUiState.ResettingPassword, vm.state.value)
        store.clear()
    }

    @Test fun asynchronousSessionExpirationUpdatesUiWithoutFakeSuccess() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.Authenticated("test-id") }
        val vm = AuthViewModel(repo)
        val store = ViewModelStore().apply { put("auth", vm) }
        vm.start(); advanceUntilIdle()
        repo.session.value = AuthSession.Unavailable(AuthFailure.NETWORK)
        runCurrent()
        assertEquals(AuthUiState.Error(AuthFailure.NETWORK), vm.state.value)
        repo.session.value = AuthSession.Verified("test-id")
        runCurrent()
        repo.session.value = AuthSession.SignedOut
        runCurrent()
        assertEquals(AuthUiState.Error(AuthFailure.SESSION_EXPIRED), vm.state.value)
        store.clear()
    }

    @Test fun failedLogoutLeavesTheRealSessionIntactAndShowsFailure() = runTest {
        val repo = FakeRepository().apply {
            session.value = AuthSession.Verified("test-id")
            exception = IOException("private transport details")
        }
        val vm = AuthViewModel(repo)
        vm.signOut(); advanceUntilIdle()
        assertEquals(AuthUiState.Error(AuthFailure.NETWORK), vm.state.value)
        assertEquals(AuthSession.Verified("test-id"), vm.session.value)
    }

    @Test fun configuredRepositoryDelegatesToInjectedBackendAndRestoresItsSession() = runTest {
        val backend = FakeRepository().apply { result = AuthResult.Authenticated("test-id") }
        val repository = SupabaseAuthRepository(SupabaseConfig("https://test.supabase.co", "sb_publishable_unit_test")) { backend }
        assertEquals(AuthResult.Ready, repository.checkSetup())
        assertEquals(AuthResult.Authenticated("test-id"), repository.restoreSession())
        assertEquals(AuthSession.Verified("test-id"), repository.session.value)
        backend.exception = IOException("private transport details")
        assertEquals(AuthResult.Failure(AuthFailure.NETWORK), repository.signOut())
        assertEquals(AuthSession.Verified("test-id"), repository.session.value)
    }

    @Test fun cryptoAndProviderFailuresHaveSanitizedCategories() {
        assertEquals(AuthFailure.STORAGE, authFailureForThrowable(AuthStorageException()))
        assertEquals(AuthFailure.STORAGE, authFailureForThrowable(java.security.GeneralSecurityException("private")))
        assertEquals(AuthFailure.NETWORK, authFailureForThrowable(IOException("private")))
        assertEquals(AuthFailure.WEAK_PASSWORD, authFailureForCode("same_password"))
        assertEquals(AuthFailure.ACCOUNT_UNAVAILABLE, authFailureForCode("email_address_not_authorized"))
        assertEquals(AuthFailure.INVALID_RECOVERY, authFailureForCode("bad_code_verifier"))
    }

    @Test fun verificationResendCooldownPreventsASecondNetworkRequest() = runTest {
        val repo = FakeRepository().apply { result = AuthResult.VerificationEmailRequested }
        val vm = AuthViewModel(repo)
        vm.resendVerification("test@example.com"); advanceUntilIdle()
        vm.resendVerification("test@example.com"); advanceUntilIdle()
        assertEquals(1, repo.calls)
        assertEquals(AuthUiState.Error(AuthFailure.RATE_LIMITED), vm.state.value)
    }
}
