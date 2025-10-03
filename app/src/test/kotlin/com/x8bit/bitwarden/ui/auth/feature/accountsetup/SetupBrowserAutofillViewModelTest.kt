package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetupBrowserAutofillViewModelTest {
    private val authRepository: AuthRepository = mockk {
        every { setOnboardingStatus(status = any()) } just runs
    }
    private val mutableBrowserThirdPartyAutofillStatusFlow =
        MutableStateFlow(DEFAULT_BROWSER_AUTOFILL_STATUS)
    private val thirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager = mockk {
        every {
            browserThirdPartyAutofillStatus
        } answers { mutableBrowserThirdPartyAutofillStatusFlow.value }
        every {
            browserThirdPartyAutofillStatusFlow
        } returns mutableBrowserThirdPartyAutofillStatusFlow
    }
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { storeShowBrowserAutofillSettingBadge(showBadge = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toSetupBrowserAutofillArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toSetupBrowserAutofillArgs)
    }

    @Test
    fun `browserThirdPartyAutofillStatusFlow should update the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableBrowserThirdPartyAutofillStatusFlow.value = DEFAULT_BROWSER_AUTOFILL_STATUS.copy(
                braveStableStatusData = BrowserThirdPartyAutoFillData(
                    isAvailable = true,
                    isThirdPartyEnabled = true,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    browserAutofillSettingsOptions = persistentListOf(
                        BrowserAutofillSettingsOption.BraveStable(enabled = true),
                        BrowserAutofillSettingsOption.ChromeStable(enabled = false),
                        BrowserAutofillSettingsOption.ChromeBeta(enabled = false),
                    ),
                ),
                awaitItem(),
            )
            mutableBrowserThirdPartyAutofillStatusFlow.value = DEFAULT_BROWSER_AUTOFILL_STATUS.copy(
                braveStableStatusData = BrowserThirdPartyAutoFillData(
                    isAvailable = false,
                    isThirdPartyEnabled = false,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    browserAutofillSettingsOptions = persistentListOf(
                        BrowserAutofillSettingsOption.ChromeStable(enabled = false),
                        BrowserAutofillSettingsOption.ChromeBeta(enabled = false),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupBrowserAutofillAction.CloseClick)
            assertEquals(SetupBrowserAutofillEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `WhyIsThisStepRequiredClick should send NavigateToBrowserIntegrationsInfo event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupBrowserAutofillAction.WhyIsThisStepRequiredClick)
                assertEquals(
                    SetupBrowserAutofillEvent.NavigateToBrowserIntegrationsInfo,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `BrowserIntegrationClick should send NavigateToBrowserAutofillSettings event`() = runTest {
        val browserPackage = BrowserPackage.BRAVE_RELEASE
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                SetupBrowserAutofillAction.BrowserIntegrationClick(browserPackage),
            )
            assertEquals(
                SetupBrowserAutofillEvent.NavigateToBrowserAutofillSettings(browserPackage),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialog should clear the dialog state`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog,
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(SetupBrowserAutofillAction.DismissDialog)
            assertEquals(
                initialState.copy(dialogState = null),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueClick should set the onboarding state to FINAL_STEP`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupBrowserAutofillAction.ContinueClick)
        verify(exactly = 1) {
            firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = false)
            authRepository.setOnboardingStatus(status = OnboardingStatus.FINAL_STEP)
        }
    }

    @Test
    fun `ContinueClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel(DEFAULT_STATE.copy(isInitialSetup = false))
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupBrowserAutofillAction.ContinueClick)
            assertEquals(SetupBrowserAutofillEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) {
            firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = false)
        }
        verify(exactly = 0) {
            authRepository.setOnboardingStatus(status = OnboardingStatus.FINAL_STEP)
        }
    }

    @Test
    fun `TurnOnLaterClick should set the onboarding state to FINAL_STEP`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `TurnOnLaterConfirmClick should set the onboarding state to FINAL_STEP`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog,
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterConfirmClick)
            assertEquals(
                initialState.copy(dialogState = null),
                awaitItem(),
            )
        }
        verify(exactly = 1) {
            firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = true)
            authRepository.setOnboardingStatus(status = OnboardingStatus.FINAL_STEP)
        }
    }

    private fun createViewModel(
        initialState: SetupBrowserAutofillState? = null,
    ): SetupBrowserAutofillViewModel = SetupBrowserAutofillViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set(key = "state", value = initialState)
            every {
                toSetupBrowserAutofillArgs()
            } returns SetupBrowserAutofillScreenArgs(isInitialSetup = true)
        },
        authRepository = authRepository,
        browserThirdPartyAutofillEnabledManager = thirdPartyAutofillEnabledManager,
        firstTimeActionManager = firstTimeActionManager,
    )
}

private val DEFAULT_BROWSER_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = true,
        isThirdPartyEnabled = false,
    ),
    chromeStableStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = true,
        isThirdPartyEnabled = false,
    ),
    chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
        isAvailable = true,
        isThirdPartyEnabled = false,
    ),
)

private val DEFAULT_STATE = SetupBrowserAutofillState(
    dialogState = null,
    isInitialSetup = true,
    browserAutofillSettingsOptions = persistentListOf(
        BrowserAutofillSettingsOption.BraveStable(enabled = false),
        BrowserAutofillSettingsOption.ChromeStable(enabled = false),
        BrowserAutofillSettingsOption.ChromeBeta(enabled = false),
    ),
)
