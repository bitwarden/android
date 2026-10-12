package com.x8bit.bitwarden.ui.platform.feature.biometrics

import app.cash.turbine.test
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
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
import javax.crypto.Cipher

class UpdateBiometricsViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns DEFAULT_USER_ID
        every { clearBiometrics(userId = DEFAULT_USER_ID) } just runs
        every { createCipherOrNull(userId = DEFAULT_USER_ID) } returns CIPHER
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { vaultTimeoutAction = any() } just runs
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `TurnOffBiometricsClick should display the disable biometrics dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(UpdateBiometricsAction.TurnOffBiometricsClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = UpdateBiometricsState.DialogState.DisableBiometricsAndClose,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialog should clear the dialog state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(UpdateBiometricsAction.TurnOffBiometricsClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = UpdateBiometricsState.DialogState.DisableBiometricsAndClose,
                ),
                awaitItem(),
            )
            viewModel.trySendAction(UpdateBiometricsAction.DismissDialog)
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmTurnOffClick should clear biometrics and emit NavigateBack when the user has a manual unlock mechanism`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(UpdateBiometricsAction.TurnOffBiometricsClick)
            viewModel.eventFlow.test {
                viewModel.trySendAction(UpdateBiometricsAction.ConfirmTurnOffClick)
                assertEquals(UpdateBiometricsEvent.NavigateBack, awaitItem())
            }
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            verify(exactly = 1) {
                authRepository.clearBiometrics(userId = DEFAULT_USER_ID)
            }
            verify(exactly = 0) {
                settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmTurnOffClick should set the vault timeout action to LOGOUT when the user has no manual unlock mechanism`() =
        runTest {
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(DEFAULT_USER_ACCOUNT.copy(hasMasterPassword = false)),
            )
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(UpdateBiometricsAction.ConfirmTurnOffClick)
                assertEquals(UpdateBiometricsEvent.NavigateBack, awaitItem())
            }
            verify(exactly = 1) {
                authRepository.clearBiometrics(userId = DEFAULT_USER_ID)
                settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmTurnOffClick should emit NavigateBack without clearing biometrics when there is no user state`() =
        runTest {
            mutableUserStateFlow.value = null
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(UpdateBiometricsAction.ConfirmTurnOffClick)
                assertEquals(UpdateBiometricsEvent.NavigateBack, awaitItem())
            }
            verify(exactly = 0) {
                authRepository.clearBiometrics(userId = any())
                settingsRepository.vaultTimeoutAction = any()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `UpdateBiometricsClick should clear biometrics and emit ShowBiometricsPrompt with the new cipher`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(UpdateBiometricsAction.ConfirmBiometricsClick)
                assertEquals(
                    UpdateBiometricsEvent.ShowBiometricsPrompt(cipher = CIPHER),
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                authRepository.clearBiometrics(userId = DEFAULT_USER_ID)
                authRepository.createCipherOrNull(userId = DEFAULT_USER_ID)
            }
        }

    @Test
    fun `UpdateBiometricsClick should display an error dialog when there is no active user`() =
        runTest {
            every { authRepository.activeUserId } returns null
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(UpdateBiometricsAction.ConfirmBiometricsClick)
                assertEquals(DEFAULT_STATE.copy(dialogState = ERROR_DIALOG_STATE), awaitItem())
            }
            verify(exactly = 0) {
                authRepository.clearBiometrics(userId = any())
                authRepository.createCipherOrNull(userId = any())
            }
        }

    @Test
    fun `UpdateBiometricsClick should display an error dialog when the cipher is null`() = runTest {
        every { authRepository.createCipherOrNull(userId = DEFAULT_USER_ID) } returns null
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(UpdateBiometricsAction.ConfirmBiometricsClick)
            assertEquals(DEFAULT_STATE.copy(dialogState = ERROR_DIALOG_STATE), awaitItem())
        }
        verify(exactly = 1) {
            authRepository.clearBiometrics(userId = DEFAULT_USER_ID)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `BiometricCipherReceived should display a loading dialog and a success dialog when setting up the key succeeds`() =
        runTest {
            coEvery {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            } returns BiometricsKeyResult.Success
            val viewModel = createViewModel()
            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
                assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
                viewModel.trySendAction(
                    UpdateBiometricsAction.BiometricCipherReceived(cipher = CIPHER),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = UpdateBiometricsState.DialogState.Loading(
                            message = BitwardenString.saving.asText(),
                        ),
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
                assertEquals(UpdateBiometricsEvent.NavigateBack, eventFlow.awaitItem())
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            }
        }

    @Test
    fun `BiometricCipherReceived should display an error dialog when setting up the key fails`() =
        runTest {
            coEvery {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            } returns BiometricsKeyResult.Error(error = Throwable("Fail!"))
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    UpdateBiometricsAction.BiometricCipherReceived(cipher = CIPHER),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = UpdateBiometricsState.DialogState.Loading(
                            message = BitwardenString.saving.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(DEFAULT_STATE.copy(dialogState = ERROR_DIALOG_STATE), awaitItem())
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            }
        }

    private fun createViewModel(): UpdateBiometricsViewModel = UpdateBiometricsViewModel(
        authRepository = authRepository,
        settingsRepository = settingsRepository,
    )
}

private const val DEFAULT_USER_ID: String = "activeUserId"
private val CIPHER: Cipher = mockk()
private val DEFAULT_STATE: UpdateBiometricsState = UpdateBiometricsState(dialogState = null)
private val ERROR_DIALOG_STATE: UpdateBiometricsState.DialogState.Error =
    UpdateBiometricsState.DialogState.Error(
        title = BitwardenString.an_error_has_occurred.asText(),
        message = BitwardenString.generic_error_message.asText(),
    )
private val DEFAULT_USER_ACCOUNT: UserState.Account = UserState.Account(
    userId = DEFAULT_USER_ID,
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environment = Environment.Prod.Us,
    isPremium = true,
    isPremiumFromSelf = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(),
    isExportable = true,
    creationDate = null,
)
private val DEFAULT_USER_STATE: UserState = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(DEFAULT_USER_ACCOUNT),
)
