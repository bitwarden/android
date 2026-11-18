package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.manager.AutofillPromptManager
import com.x8bit.bitwarden.data.autofill.manager.FillAssistManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoFillViewModelTest : BaseViewModelTest() {

    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(false)
    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(false)

    private val authRepository: AuthRepository = mockk {
        every { userStateFlow.value?.activeUserId } returns "activeUserId"
    }

    private val mutableAutofillPromptStatusFlow =
        MutableStateFlow<AutofillPromptStatus>(AutofillPromptStatus.On)
    private val autofillPromptManager: AutofillPromptManager = mockk {
        every { autofillPromptStatusFlow } returns mutableAutofillPromptStatusFlow
    }

    private val mutableChromeAutofillStatusFlow = MutableStateFlow(DEFAULT_AUTOFILL_STATUS)
    private val browserThirdPartyAutofillEnabledManager =
        mockk<BrowserThirdPartyAutofillEnabledManager> {
            every { browserThirdPartyAutofillStatusFlow } returns mutableChromeAutofillStatusFlow
            every { browserThirdPartyAutofillStatus } returns DEFAULT_AUTOFILL_STATUS
        }

    private val mutableFillAssistTargetingRulesFlagFlow = MutableStateFlow(false)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlag(FlagKey.FillAssistTargetingRules)
        } answers {
            mutableFillAssistTargetingRulesFlagFlow.value
        }
        every {
            getFeatureFlagFlow(FlagKey.FillAssistTargetingRules)
        } returns mutableFillAssistTargetingRulesFlagFlow
    }

    private val fillAssistManager: FillAssistManager = mockk(relaxed = true)

    private val mutableIsFillAssistEnabledFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk {
        every { isInlineAutofillEnabled } returns true
        every { isInlineAutofillEnabled = any() } just runs
        every { isAutoCopyTotpDisabled } returns true
        every { isAutoCopyTotpDisabled = any() } just runs
        every { isAutofillSavePromptDisabled } returns true
        every { isAutofillSavePromptDisabled = any() } just runs
        every { isFillAssistEnabled } answers { mutableIsFillAssistEnabledFlow.value }
        every {
            isFillAssistEnabled = any()
        } answers { mutableIsFillAssistEnabledFlow.value = firstArg() }
        every { isFillAssistEnabledFlow } returns mutableIsFillAssistEnabledFlow
        every { defaultUriMatchType } returns UriMatchType.DOMAIN
        every { defaultUriMatchType = any() } just runs
        every { isAccessibilityEnabledStateFlow } returns mutableIsAccessibilityEnabledStateFlow
        every { isAutofillEnabledStateFlow } returns mutableIsAutofillEnabledStateFlow
        every { disableAutofill() } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(Build.VERSION_CODES.R) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `initial state should be correct when not set`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns true

        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns true

        mutableIsAutofillEnabledStateFlow.value = true
        val state = DEFAULT_STATE.copy(
            isAutoFillServicesEnabled = true,
            defaultUriMatchType = UriMatchType.REGULAR_EXPRESSION,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when sdk is below min`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns false

        val expected = DEFAULT_STATE.copy(
            showPasskeyManagementRow = false,
        )
        val viewModel = createViewModel(state = null)

        assertEquals(expected, viewModel.stateFlow.value)
    }

    @Test
    fun `showInlineAutofillOption should be true when the build version is not below R`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.R) } returns true
        val viewModel = createViewModel(state = null)
        assertEquals(
            DEFAULT_STATE.copy(
                showInlineAutofillOption = true,
                showPasskeyManagementRow = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `changes in accessibility enabled status should update the state`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        mutableIsAccessibilityEnabledStateFlow.value = true

        assertEquals(
            DEFAULT_STATE.copy(isAccessibilityAutofillEnabled = true),
            viewModel.stateFlow.value,
        )

        mutableIsAccessibilityEnabledStateFlow.value = false

        assertEquals(
            DEFAULT_STATE.copy(isAccessibilityAutofillEnabled = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `changes in autofill enabled status should update the state`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        mutableIsAutofillEnabledStateFlow.value = true

        assertEquals(
            DEFAULT_STATE.copy(isAutoFillServicesEnabled = true),
            viewModel.stateFlow.value,
        )

        mutableIsAutofillEnabledStateFlow.value = false

        assertEquals(
            DEFAULT_STATE.copy(isAutoFillServicesEnabled = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AskToAddLoginClick should update the state and save the new value to settings`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(true))
        assertEquals(
            DEFAULT_STATE.copy(isAskToAddLoginEnabled = true),
            viewModel.stateFlow.value,
        )
        // The UI enables the value, so the value gets flipped to save it as a "disabled" value.
        verify { settingsRepository.isAutofillSavePromptDisabled = false }
    }

    @Test
    fun `on HelpCardClick should emit NavigateToAutofillHelp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.HelpCardClick)
            assertEquals(AutoFillEvent.NavigateToAutofillHelp, awaitItem())
        }
    }

    @Test
    fun `on AutoFillServicesClick with false should disable autofill`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(false))
        verify {
            settingsRepository.disableAutofill()
        }
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on UseAccessibilityAutofillClick should emit NavigateToAccessibilitySettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick)
            assertEquals(AutoFillEvent.NavigateToAccessibilitySettings, awaitItem())
        }
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on AutoFillServicesClick with true should emit NavigateToAutofillSettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true))
            assertEquals(
                AutoFillEvent.NavigateToAutofillSettings,
                awaitItem(),
            )
        }
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BackClick)
            assertEquals(AutoFillEvent.NavigateBack, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on CopyTotpAutomaticallyClick should update the isCopyTotpAutomaticallyEnabled state and save new value to settings`() =
        runTest {
            val viewModel = createViewModel()
            val isEnabled = true
            viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(isEnabled))
            viewModel.eventFlow.test {
                expectNoEvents()
            }
            assertEquals(
                DEFAULT_STATE.copy(isCopyTotpAutomaticallyEnabled = isEnabled),
                viewModel.stateFlow.value,
            )

            // The UI enables the value, so the value gets flipped to save it as a "disabled" value.
            verify { settingsRepository.isAutoCopyTotpDisabled = !isEnabled }
        }

    @Test
    fun `on AutofillStyleSelected should update the state and save the new value to settings`() {
        val viewModel = createViewModel()
        val autofillStyle = AutofillStyle.POPUP
        viewModel.trySendAction(AutoFillAction.AutofillStyleSelected(style = autofillStyle))
        assertEquals(
            DEFAULT_STATE.copy(autofillStyle = autofillStyle),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.isInlineAutofillEnabled = false }
    }

    @Test
    fun `on PasskeyManagementClick should emit NavigateToSettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.PasskeyManagementClick)
            assertEquals(AutoFillEvent.NavigateToSettings, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on DefaultUriMatchTypeSelect should update the state and save the new value to settings`() {
        val viewModel = createViewModel()
        val method = UriMatchType.EXACT
        viewModel.trySendAction(AutoFillAction.DefaultUriMatchTypeSelect(method))
        assertEquals(
            DEFAULT_STATE.copy(defaultUriMatchType = method),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.defaultUriMatchType = method }
    }

    @Test
    fun `on BlockAutoFillClick should emit NavigateToBlockAutoFill`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BlockAutoFillClick)
            assertEquals(AutoFillEvent.NavigateToBlockAutoFill, awaitItem())
        }
    }

    @Test
    fun `when autofillPromptStatusFlow updates value, should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            mutableAutofillPromptStatusFlow.value = AutofillPromptStatus.AutofillOff
            assertEquals(
                DEFAULT_STATE.copy(autofillPromptStatus = AutofillPromptStatus.AutofillOff),
                awaitItem(),
            )

            val browserOff = AutofillPromptStatus.BrowserOff(
                browserPackage = BrowserPackage.CHROME_STABLE,
            )
            mutableAutofillPromptStatusFlow.value = browserOff
            assertEquals(DEFAULT_STATE.copy(autofillPromptStatus = browserOff), awaitItem())

            mutableAutofillPromptStatusFlow.value = AutofillPromptStatus.On
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `ctaState should map the autofill prompt status to a call to action`() {
        mapOf(
            AutofillPromptStatus.AutofillOff to CtaState.Autofill,
            AutofillPromptStatus.BrowserOff(
                browserPackage = BrowserPackage.CHROME_STABLE,
            ) to CtaState.BrowserAutofill(browserPackage = BrowserPackage.CHROME_STABLE),
            AutofillPromptStatus.On to CtaState.Default,
        )
            .forEach { (prompt, expected) ->
                assertEquals(expected, DEFAULT_STATE.copy(autofillPromptStatus = prompt).ctaState)
            }
    }

    @Test
    fun `AutofillActionCardCtaClick should emit NavigateToSetupAutofill`() = runTest {
        mutableAutofillPromptStatusFlow.value = AutofillPromptStatus.AutofillOff
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AutofillActionCardCtaClick)
            assertEquals(AutoFillEvent.NavigateToSetupAutofill, awaitItem())
        }
    }

    @Test
    fun `BrowserAutofillActionCardCtaClick should emit NavigateToSetupBrowserAutofill`() = runTest {
        mutableAutofillPromptStatusFlow.value = AutofillPromptStatus.BrowserOff(
            browserPackage = BrowserPackage.CHROME_STABLE,
        )
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BrowserAutofillActionCardCtaClick)
            assertEquals(AutoFillEvent.NavigateToSetupBrowserAutofill, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when ChromeAutofillStatusReceive with updated information is processed state updates as expected`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )
                mutableChromeAutofillStatusFlow.update {
                    it.copy(
                        chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA.copy(
                            isAvailable = true,
                        ),
                    )
                }
                assertEquals(
                    DEFAULT_STATE.copy(
                        browserAutofillSettingsOptions = persistentListOf(
                            BrowserAutofillSettingsOption.ChromeStable(enabled = false),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when ChromeAutofillSelected action is handled the correct NavigateToBrowserAutofillSettings event is sent`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AutoFillAction.BrowserAutofillSelected(BrowserPackage.CHROME_STABLE),
                )
                assertEquals(
                    AutoFillEvent.NavigateToBrowserAutofillSettings(BrowserPackage.CHROME_STABLE),
                    awaitItem(),
                )
                viewModel.trySendAction(
                    AutoFillAction.BrowserAutofillSelected(BrowserPackage.CHROME_BETA),
                )
                assertEquals(
                    AutoFillEvent.NavigateToBrowserAutofillSettings(BrowserPackage.CHROME_BETA),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when AboutPrivilegedAppsClick action is handled the correct NavigateToAboutPrivilegedAppsScreen event is sent`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.AboutPrivilegedAppsClick)
                assertEquals(
                    AutoFillEvent.NavigateToAboutPrivilegedAppsScreen,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when PrivilegedAppsClick action is handled the correct NavigateToPrivilegedAppsListScreen event is sent`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.PrivilegedAppsClick)
                assertEquals(
                    AutoFillEvent.NavigateToPrivilegedAppsListScreen,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `when LearnMoreClick action is handled NavigateToLearnMore event is sent`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.LearnMoreClick)
                assertEquals(
                    AutoFillEvent.NavigateToLearnMore,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `FillAssistToggleClick with isEnabled true persists setting and triggers sync`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AutoFillAction.FillAssistToggleClick(isEnabled = true))
            assertEquals(DEFAULT_STATE.copy(isFillAssistEnabled = true), awaitItem())
        }
        verify { settingsRepository.isFillAssistEnabled = true }
        verify { fillAssistManager.syncIfNecessary() }
    }

    @Test
    fun `FillAssistToggleClick with isEnabled false persists setting and skips sync`() = runTest {
        mutableIsFillAssistEnabledFlow.value = true
        val enabledState = DEFAULT_STATE.copy(isFillAssistEnabled = true)
        val viewModel = createViewModel(state = enabledState)
        viewModel.stateFlow.test {
            assertEquals(enabledState, awaitItem())
            viewModel.trySendAction(AutoFillAction.FillAssistToggleClick(isEnabled = false))
            assertEquals(enabledState.copy(isFillAssistEnabled = false), awaitItem())
        }
        verify { settingsRepository.isFillAssistEnabled = false }
        verify(exactly = 0) { fillAssistManager.syncIfNecessary() }
    }

    @Test
    fun `when isFillAssistEnabled updates in the repository, should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableIsFillAssistEnabledFlow.value = true
            assertEquals(DEFAULT_STATE.copy(isFillAssistEnabled = true), awaitItem())
        }
    }

    @Test
    fun `when FillAssistTargetingRules flag updates, should update showFillAssistOption state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                mutableFillAssistTargetingRulesFlagFlow.value = true
                assertEquals(DEFAULT_STATE.copy(showFillAssistOption = true), awaitItem())
            }
        }

    @Test
    fun `FillAssistInfoClick emits NavigateToFillAssistHelp event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.FillAssistInfoClick)
            assertEquals(AutoFillEvent.NavigateToFillAssistHelp, awaitItem())
        }
    }

    private fun createViewModel(
        state: AutoFillState? = DEFAULT_STATE,
    ): AutoFillViewModel = AutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        settingsRepository = settingsRepository,
        authRepository = authRepository,
        autofillPromptManager = autofillPromptManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
        featureFlagManager = featureFlagManager,
        fillAssistManager = fillAssistManager,
    )
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    showFillAssistOption = false,
    isFillAssistEnabled = false,
    isAskToAddLoginEnabled = false,
    isAccessibilityAutofillEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    autofillStyle = AutofillStyle.INLINE,
    showInlineAutofillOption = false,
    showPasskeyManagementRow = true,
    defaultUriMatchType = UriMatchType.DOMAIN,
    autofillPromptStatus = AutofillPromptStatus.On,
    activeUserId = "activeUserId",
    browserAutofillSettingsOptions = persistentListOf(),
)

private val DEFAULT_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    vivaldiStableChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    defaultBrowserPackageName = null,
)
