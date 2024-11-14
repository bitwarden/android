package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

class SetupUnlockViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { setOnboardingStatus(userId = any(), status = any()) } just runs
    }

    private val mutableAutofillEnabledStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { isUnlockWithPinEnabled } returns false
        every { isUnlockWithBiometricsEnabled } returns false
        every { isAutofillEnabledStateFlow } returns mutableAutofillEnabledStateFlow
    }
    private val mutableFirstTimeStateFlow = MutableStateFlow(FirstTimeState())
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowUnlockSettingBadge(any()) } just runs
    }
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk {
        every { getOrCreateCipher(userId = DEFAULT_USER_ID) } returns CIPHER
        every {
            isBiometricIntegrityValid(userId = DEFAULT_USER_ID, cipher = CIPHER)
        } returns false
        every { createCipherOrNull(DEFAULT_USER_ID) } returns CIPHER
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when not initial setup`() {
        val viewModel = createViewModel(DEFAULT_STATE.copy(isInitialSetup = false))
        assertEquals(
            DEFAULT_STATE.copy(isInitialSetup = false),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueClick should call setOnboardingStatus and set to AUTOFILL_SETUP if AutoFill is not enabled`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupUnlockAction.ContinueClick)
        verify {
            authRepository.setOnboardingStatus(
                userId = DEFAULT_USER_ID,
                status = OnboardingStatus.AUTOFILL_SETUP,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueClick should send NavigateBack event if this is not the initial setup and set first time value to false`() =
        runTest {
            val viewModel = createViewModel(DEFAULT_STATE.copy(isInitialSetup = false))
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupUnlockAction.ContinueClick)
                assertEquals(SetupUnlockEvent.NavigateBack, awaitItem())
            }
            verify(exactly = 1) {
                firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
            }
            verify(exactly = 0) {
                authRepository.setOnboardingStatus(
                    userId = DEFAULT_USER_ID,
                    status = OnboardingStatus.AUTOFILL_SETUP,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SetUpLaterClick should call setOnboardingStatus and set to AUTOFILL_SETUP if AutoFill is not enabled`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupUnlockAction.SetUpLaterClick)
        verify {
            authRepository.setOnboardingStatus(
                userId = DEFAULT_USER_ID,
                status = OnboardingStatus.AUTOFILL_SETUP,
            )
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = true)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueClick should call setOnboardingStatus and set to FINAL_STEP if AutoFill is already enabled and set first time value to false`() {
        mutableAutofillEnabledStateFlow.update { true }
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupUnlockAction.ContinueClick)
        verify(exactly = 1) {
            authRepository.setOnboardingStatus(
                userId = DEFAULT_USER_ID,
                status = OnboardingStatus.FINAL_STEP,
            )
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SetUpLaterClick should call setOnboardingStatus and set to FINAL_STEP if AutoFill is already enabled`() =
        runTest {
            mutableAutofillEnabledStateFlow.update { true }
            val viewModel = createViewModel()
            viewModel.trySendAction(SetupUnlockAction.SetUpLaterClick)
            verify {
                authRepository.setOnboardingStatus(
                    userId = DEFAULT_USER_ID,
                    status = OnboardingStatus.FINAL_STEP,
                )
            }
        }

    @Test
    fun `on UnlockWithBiometricToggle false should call clearBiometricsKey and update the state`() {
        val initialState = DEFAULT_STATE.copy(isUnlockWithBiometricsEnabled = true)
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
        every { settingsRepository.clearBiometricsKey() } just runs
        val viewModel = createViewModel(initialState)
        assertEquals(initialState, viewModel.stateFlow.value)

        viewModel.trySendAction(SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = false))

        assertEquals(
            initialState.copy(isUnlockWithBiometricsEnabled = false),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            settingsRepository.clearBiometricsKey()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggle true and setupBiometricsKey error should update the state accordingly`() =
        runTest {
            coEvery { settingsRepository.setupBiometricsKey() } returns BiometricsKeyResult.Error
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = true),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SetupUnlockState.DialogState.Loading(
                            title = R.string.saving.asText(),
                        ),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = null,
                        isUnlockWithBiometricsEnabled = false,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggle true and setupBiometricsKey success should call update the state accordingly`() =
        runTest {
            coEvery { settingsRepository.setupBiometricsKey() } returns BiometricsKeyResult.Success
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = true),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SetupUnlockState.DialogState.Loading(
                            title = R.string.saving.asText(),
                        ),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = null,
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(isUnlockWithPinEnabled = true)
        every { settingsRepository.clearUnlockPin() } just runs
        val viewModel = createViewModel(state = initialState)
        viewModel.trySendAction(SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.Disabled))
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = false),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            settingsRepository.clearUnlockPin()
        }
    }

    @Test
    fun `on UnlockWithPinToggle PendingEnabled should set pin unlock to true`() {
        val initialState = DEFAULT_STATE.copy(isUnlockWithPinEnabled = false)
        val viewModel = createViewModel(state = initialState)
        viewModel.trySendAction(
            SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Enabled should set pin unlock to true and set the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(isUnlockWithPinEnabled = false)
        every { settingsRepository.storeUnlockPin(any(), any()) } just runs

        val viewModel = createViewModel(state = initialState)
        viewModel.trySendAction(
            SetupUnlockAction.UnlockWithPinToggle(
                UnlockWithPinState.Enabled(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = true,
                ),
            ),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.storeUnlockPin(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = true,
            )
        }
    }

    @Test
    fun `on DismissDialog should hide dialog`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(SetupUnlockAction.DismissDialog)

        assertEquals(
            DEFAULT_STATE.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `EnableBiometricsClick action should create a new biometrics cipher and emit result`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(SetupUnlockAction.EnableBiometricsClick)

            verify {
                biometricsEncryptionManager.getOrCreateCipher(DEFAULT_USER_ID)
            }

            viewModel.eventFlow.test {
                assertEquals(
                    SetupUnlockEvent.ShowBiometricsPrompt(CIPHER),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `EnableBiometricsClick actin should show error dialog when cipher is null`() {
        every {
            biometricsEncryptionManager.createCipherOrNull(DEFAULT_USER_ID)
        } returns null
        val viewModel = createViewModel()

        viewModel.trySendAction(SetupUnlockAction.EnableBiometricsClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SetupUnlockState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CloseClick action should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupUnlockAction.CloseClick)
            assertEquals(
                SetupUnlockEvent.NavigateBack,
                awaitItem(),
            )
        }
        verify(exactly = 0) {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CloseClick action should update the first time state to false if continue button is enabled`() =
        runTest {
            val viewModel =
                createViewModel(state = DEFAULT_STATE.copy(isUnlockWithPinEnabled = true))
            viewModel.trySendAction(SetupUnlockAction.CloseClick)
            verify {
                firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
            }
        }

    private fun createViewModel(
        state: SetupUnlockState? = null,
    ): SetupUnlockViewModel =
        SetupUnlockViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "state" to state,
                    "isInitialSetup" to true,
                ),
            ),
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            biometricsEncryptionManager = biometricsEncryptionManager,
            firstTimeActionManager = firstTimeActionManager,
        )
}

private const val DEFAULT_USER_ID: String = "activeUserId"
private val DEFAULT_STATE: SetupUnlockState = SetupUnlockState(
    userId = DEFAULT_USER_ID,
    isUnlockWithPinEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithBiometricsEnabled = false,
    dialogState = null,
    isInitialSetup = true,
)

private val DEFAULT_USER_ACCOUNT = UserState.Account(
    userId = DEFAULT_USER_ID,
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environment = Environment.Us,
    isPremium = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
)

private val CIPHER = mockk<Cipher>()
private val DEFAULT_USER_STATE: UserState = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        DEFAULT_USER_ACCOUNT,
    ),
)
