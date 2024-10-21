package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TrustedDeviceViewModelTest : BaseViewModelTest() {

    private val mutableAuthStateFlow = MutableStateFlow(DEFAULT_AUTH_STATE)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { authStateFlow } returns mutableAuthStateFlow
        every { userStateFlow } returns mutableUserStateFlow
        every { shouldTrustDevice = any() } just runs
        every { logout() } just runs
    }
    private val environmentRepo: FakeEnvironmentRepository = FakeEnvironmentRepository()

    @Test
    fun `on init should logout when Uninitialized`() {
        mutableAuthStateFlow.value = AuthState.Uninitialized
        createViewModel()

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Test
    fun `on init should logout when unauthenticated`() {
        mutableAuthStateFlow.value = AuthState.Unauthenticated
        createViewModel()

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Test
    fun `on init should logout when trusted device is not present`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(trustedDevice = null)),
        )
        createViewModel()

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Test
    fun `on BackClick should logout`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(TrustedDeviceAction.BackClick)

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Test
    fun `on DismissDialog should clear dialogState`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = TrustedDeviceState.DialogState.Loading("Loading".asText()),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(TrustedDeviceAction.DismissDialog)

        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `on RememberToggle updates the isRemembered state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(TrustedDeviceAction.RememberToggle(isRemembered = false))
            assertEquals(DEFAULT_STATE.copy(isRemembered = false), awaitItem())
            viewModel.trySendAction(TrustedDeviceAction.RememberToggle(isRemembered = true))
            assertEquals(DEFAULT_STATE.copy(isRemembered = true), awaitItem())
        }
    }

    @Test
    fun `on ContinueClick with createNewSsoUser failure should display the error dialog state`() =
        runTest {
            every { authRepository.shouldTrustDevice = true } just runs
            coEvery { authRepository.createNewSsoUser() } returns NewSsoUserResult.Failure
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(TrustedDeviceAction.ContinueClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TrustedDeviceState.DialogState.Loading(
                            message = R.string.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TrustedDeviceState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                authRepository.shouldTrustDevice = true
            }
            coVerify(exactly = 1) {
                authRepository.createNewSsoUser()
            }
        }

    @Test
    fun `on ContinueClick with createNewSsoUser success should display the loading dialog state`() =
        runTest {
            every { authRepository.shouldTrustDevice = true } just runs
            coEvery { authRepository.createNewSsoUser() } returns NewSsoUserResult.Success
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(TrustedDeviceAction.ContinueClick)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = TrustedDeviceState.DialogState.Loading(
                            message = R.string.loading.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(DEFAULT_STATE, awaitItem())
            }
            verify(exactly = 1) {
                authRepository.shouldTrustDevice = true
            }
            coVerify(exactly = 1) {
                authRepository.createNewSsoUser()
            }
        }

    @Test
    fun `on ApproveWithAdminClick emits NavigateToApproveWithAdmin`() = runTest {
        every { authRepository.shouldTrustDevice = true } just runs
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithAdminClick)
            assertEquals(TrustedDeviceEvent.NavigateToApproveWithAdmin(email = EMAIL), awaitItem())
        }
        verify(exactly = 1) {
            authRepository.shouldTrustDevice = true
        }
    }

    @Test
    fun `on ApproveWithDeviceClick emits NavigateToApproveWithDevice`() = runTest {
        every { authRepository.shouldTrustDevice = true } just runs
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithDeviceClick)
            assertEquals(TrustedDeviceEvent.NavigateToApproveWithDevice(email = EMAIL), awaitItem())
        }
        verify(exactly = 1) {
            authRepository.shouldTrustDevice = true
        }
    }

    @Test
    fun `on ApproveWithPasswordClick emits NavigateToLockScreen`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithPasswordClick)
            assertEquals(TrustedDeviceEvent.NavigateToLockScreen(email = EMAIL), awaitItem())
        }
        verify(exactly = 1) {
            authRepository.shouldTrustDevice = true
        }
    }

    @Test
    fun `on NotYouClick should logout`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(TrustedDeviceAction.NotYouClick)

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    private fun createViewModel(
        state: TrustedDeviceState? = null,
        environmentRepository: EnvironmentRepository = environmentRepo,
        authorizationRepository: AuthRepository = authRepository,
    ): TrustedDeviceViewModel =
        TrustedDeviceViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
                set("email_address", "email@bitwarden.com")
            },
            environmentRepository = environmentRepository,
            authRepository = authorizationRepository,
        )
}

private const val USER_ID: String = "userId"
private const val EMAIL: String = "email@bitwarden.com"

private val DEFAULT_STATE: TrustedDeviceState = TrustedDeviceState(
    dialogState = null,
    emailAddress = EMAIL,
    environmentLabel = "bitwarden.com",
    isRemembered = true,
    showContinueButton = false,
    showOtherDeviceButton = true,
    showRequestAdminButton = true,
    showMasterPasswordButton = false,
)

private val DEFAULT_AUTH_STATE: AuthState = AuthState.Authenticated(accessToken = "accessToken")

private val TRUSTED_DEVICE = UserState.TrustedDevice(
    isDeviceTrusted = false,
    hasAdminApproval = true,
    hasLoginApprovingDevice = true,
    hasResetPasswordPermission = false,
)

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = USER_ID,
    name = "Active User",
    email = EMAIL,
    environment = Environment.Us,
    avatarColorHex = "#aa00aa",
    isPremium = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = TRUSTED_DEVICE,
    hasMasterPassword = false,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = USER_ID,
    accounts = listOf(DEFAULT_ACCOUNT),
)
