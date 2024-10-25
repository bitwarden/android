package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
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

    private val mutableFirstTimeStateFlow = MutableStateFlow(FirstTimeState())
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowAutoFillSettingBadge(any()) } just runs
    }

    private val settingsRepository: SettingsRepository = mockk {
        every { isInlineAutofillEnabled } returns true
        every { isInlineAutofillEnabled = any() } just runs
        every { isAutoCopyTotpDisabled } returns true
        every { isAutoCopyTotpDisabled = any() } just runs
        every { isAutofillSavePromptDisabled } returns true
        every { isAutofillSavePromptDisabled = any() } just runs
        every { defaultUriMatchType } returns UriMatchType.DOMAIN
        every { defaultUriMatchType = any() } just runs
        every { isAccessibilityEnabledStateFlow } returns mutableIsAccessibilityEnabledStateFlow
        every { isAutofillEnabledStateFlow } returns mutableIsAutofillEnabledStateFlow
        every { disableAutofill() } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.R) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `initial state should be correct when not set`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns false

        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `initial state should be correct when set`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns false

        mutableIsAutofillEnabledStateFlow.value = true
        val state = DEFAULT_STATE.copy(
            isAutoFillServicesEnabled = true,
            defaultUriMatchType = UriMatchType.REGULAR_EXPRESSION,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)

        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `initial state should be correct when sdk is below min`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns true

        val expected = DEFAULT_STATE.copy(
            showPasskeyManagementRow = false,
        )
        val viewModel = createViewModel(state = null)

        assertEquals(expected, viewModel.stateFlow.value)

        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `showInlineAutofillOption should be true when the build version is not below R`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.R) } returns false
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
    fun `on AutoFillServicesClick should update show autofill in repository if card shown`() {
        mutableFirstTimeStateFlow.update { it.copy(showSetupAutofillCard = true) }
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(showAutofillActionCard = true),
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true))
        verify(exactly = 1) {
            firstTimeActionManager.storeShowAutoFillSettingBadge(
                false,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on AutoFillServicesClick should not update show autofill in repository if card not shown`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(showAutofillActionCard = false),
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true))
        verify(exactly = 0) {
            firstTimeActionManager.storeShowAutoFillSettingBadge(
                false,
            )
        }
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
    fun `on UseInlineAutofillClick should update the state and save the new value to settings`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(false))
        assertEquals(
            DEFAULT_STATE.copy(isUseInlineAutoFillEnabled = false),
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
    fun `when showAutofillBadgeFlow updates value, should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableFirstTimeStateFlow.update { it.copy(showSetupAutofillCard = true) }
            assertEquals(DEFAULT_STATE.copy(showAutofillActionCard = true), awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when AutoFillActionCardCtaClick action is sent should update show autofill in repository and send NavigateToSetupAutofill event`() =
        runTest {
            mutableFirstTimeStateFlow.update { it.copy(showSetupAutofillCard = true) }
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.AutoFillActionCardCtaClick)
                assertEquals(
                    AutoFillEvent.NavigateToSetupAutofill,
                    awaitItem(),
                )
            }
            verify(exactly = 0) {
                firstTimeActionManager.storeShowAutoFillSettingBadge(false)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when DismissShowAutofillActionCard action is sent should update show autofill in repository`() {
        mutableFirstTimeStateFlow.update { it.copy(showSetupAutofillCard = true) }
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.DismissShowAutofillActionCard)
        verify {
            firstTimeActionManager.storeShowAutoFillSettingBadge(
                false,
            )
        }
    }

    private fun createViewModel(
        state: AutoFillState? = DEFAULT_STATE,
    ): AutoFillViewModel = AutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        settingsRepository = settingsRepository,
        authRepository = authRepository,
        firstTimeActionManager = firstTimeActionManager,
    )
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAccessibilityAutofillEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    isUseInlineAutoFillEnabled = true,
    showInlineAutofillOption = false,
    showPasskeyManagementRow = true,
    defaultUriMatchType = UriMatchType.DOMAIN,
    showAutofillActionCard = false,
    activeUserId = "activeUserId",
)
