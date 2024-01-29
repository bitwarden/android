package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoFillScreenTest : BaseComposeTest() {

    private var isSystemSettingsRequestSuccess = false
    private var onNavigateBackCalled = false
    private var onNavigateToBlockAutoFillScreenCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<AutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { startSystemAutofillSettingsActivity() } answers { isSystemSettingsRequestSuccess }
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            AutoFillScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToBlockAutoFillScreen = { onNavigateToBlockAutoFillScreenCalled = true },
                viewModel = viewModel,
                intentManager = intentManager,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToAutofillSettings should attempt to navigate to system settings and not show the fallback dialog when result is a success`() {
        isSystemSettingsRequestSuccess = true

        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAutofillSettings)

        verify {
            intentManager.startSystemAutofillSettingsActivity()
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToAutofillSettings should attempt to navigate to system settings and show the fallback dialog when result is not a success`() {
        isSystemSettingsRequestSuccess = false

        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAutofillSettings)

        verify {
            intentManager.startSystemAutofillSettingsActivity()
        }

        composeTestRule
            .onAllNodesWithText(
                "We were unable to automatically open the Android autofill settings menu for " +
                    "you. You can navigate to the autofill settings menu manually from Android " +
                    "Settings > System > Languages and input > Advanced > Autofill service.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on autofill settings fallback dialog Ok click should dismiss the dialog`() {
        isSystemSettingsRequestSuccess = false
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAutofillSettings)

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on auto fill services toggle should send AutoFillServicesClick`() {
        composeTestRule
            .onNodeWithText("Auto-fill services")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true)) }
    }

    @Test
    fun `auto fill services should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Auto-fill services")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isAutoFillServicesEnabled = true) }
        composeTestRule
            .onNodeWithText("Auto-fill services")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `on use inline auto fill toggle should send UseInlineAutofillClick`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                isUseInlineAutoFillEnabled = false,
            )
        }
        composeTestRule
            .onNodeWithText("Use inline autofill")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(true)) }
    }

    @Test
    fun `use inline autofill should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Use inline autofill")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isUseInlineAutoFillEnabled = true) }
        composeTestRule
            .onNodeWithText("Use inline autofill")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `use inline autofill should be disabled or enabled according to state`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                isUseInlineAutoFillEnabled = true,
            )
        }

        composeTestRule
            .onNodeWithText("Use inline autofill")
            .performScrollTo()
            .assertIsOn()
            .assertIsEnabled()

        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = false,
                isUseInlineAutoFillEnabled = true,
            )
        }

        composeTestRule
            .onNodeWithText("Use inline autofill")
            .performScrollTo()
            .assertIsOn()
            .assertIsNotEnabled()
    }

    @Test
    fun `on copy TOTP automatically toggle should send CopyTotpAutomaticallyClick`() {
        composeTestRule
            .onNodeWithText("Copy TOTP automatically")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(true)) }
    }

    @Test
    fun `copy TOTP automatically should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Copy TOTP automatically")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isCopyTotpAutomaticallyEnabled = true) }
        composeTestRule
            .onNodeWithText("Copy TOTP automatically")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `on ask to add login toggle should send AskToAddLoginClick`() {
        composeTestRule
            .onNodeWithText("Ask to add login")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(true)) }
    }

    @Test
    fun `ask to add login should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Ask to add login")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isAskToAddLoginEnabled = true) }
        composeTestRule
            .onNodeWithText("Ask to add login")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `on default URI match type click should display dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText("Default URI match detection")
            .performScrollTo()
            .assert(!hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule
            .onAllNodesWithText("Default URI match detection")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on default URI match type dialog item click should send DefaultUriMatchTypeSelect and close the dialog`() {
        composeTestRule
            .onNodeWithText("Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Exact")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AutoFillAction.DefaultUriMatchTypeSelect(
                    defaultUriMatchType = UriMatchType.EXACT,
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on default URI match type dialog cancel click should close the dialog`() {
        composeTestRule
            .onNodeWithText("Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `default URI match type should update according to state`() {
        composeTestRule
            .onNodeWithText("Base domain")
            .assertExists()
        composeTestRule
            .onNodeWithText("Starts with")
            .assertDoesNotExist()
        mutableStateFlow.update {
            it.copy(defaultUriMatchType = UriMatchType.STARTS_WITH)
        }
        composeTestRule
            .onNodeWithText("Base domain")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Starts with")
            .assertExists()
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AutoFillAction.BackClick) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on block auto fill click should send BlockAutoFillClick`() {
        composeTestRule
            .onNodeWithText("Block auto-fill")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) }
    }

    @Test
    fun `on NavigateToBlockAutoFill should call onNavigateToBlockAutoFillScreen`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToBlockAutoFill)
        assertTrue(onNavigateToBlockAutoFillScreenCalled)
    }
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    isUseInlineAutoFillEnabled = false,
    defaultUriMatchType = UriMatchType.DOMAIN,
)
