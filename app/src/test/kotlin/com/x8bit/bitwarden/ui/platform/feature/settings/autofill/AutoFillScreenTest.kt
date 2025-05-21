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
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeReleaseChannel
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.chrome.model.ChromeAutofillSettingsOption
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoFillScreenTest : BitwardenComposeTest() {

    private var isSystemSettingsRequestSuccess = false
    private var onNavigateBackCalled = false
    private var onNavigateToBlockAutoFillScreenCalled = false
    private var onNavigateToSetupAutoFillScreenCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<AutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { startSystemAutofillSettingsActivity() } answers { isSystemSettingsRequestSuccess }
        every { startCredentialManagerSettings(any()) } just runs
        every { startSystemAccessibilitySettingsActivity() } just runs
        every { startChromeAutofillSettingsActivity(any()) } returns true
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
        ) {
            AutoFillScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToBlockAutoFillScreen = { onNavigateToBlockAutoFillScreenCalled = true },
                onNavigateToSetupAutofill = { onNavigateToSetupAutoFillScreenCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateToAccessibilitySettings should attempt to navigate to system settings`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAccessibilitySettings)

        verify(exactly = 1) {
            intentManager.startSystemAccessibilitySettingsActivity()
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
    fun `on NavigateToSettings should attempt to navigate to credential manager settings`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToSettings)

        verify { intentManager.startCredentialManagerSettings(any()) }

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on use accessibility click with accessibility already enabled should emit UseAccessibilityAutofillClick`() {
        mutableStateFlow.update { it.copy(isAccessibilityAutofillEnabled = true) }
        composeTestRule
            .onNodeWithText(text = "Use accessibility")
            .performScrollTo()
            .performClick()

        composeTestRule.assertNoDialogExists()
        verify(exactly = 1) {
            viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on use accessibility click with accessibility already disabled should display disclosure dialog and declining closes the dialog`() {
        mutableStateFlow.update { it.copy(isAccessibilityAutofillEnabled = false) }
        composeTestRule
            .onNodeWithText(text = "Use accessibility")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Accessibility Service Disclosure")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Decline")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 0) {
            viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on use accessibility click with accessibility already disabled should display disclosure dialog and accepting closes the dialog and emits UseAccessibilityAutofillClick`() {
        mutableStateFlow.update { it.copy(isAccessibilityAutofillEnabled = false) }
        composeTestRule
            .onNodeWithText(text = "Use accessibility")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Accessibility Service Disclosure")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Accept")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick)
        }
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
            .onNodeWithText("Autofill services")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true)) }
    }

    @Test
    fun `auto fill services should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Autofill services")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isAutoFillServicesEnabled = true) }
        composeTestRule
            .onNodeWithText("Autofill services")
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

    @Suppress("MaxLineLength")
    @Test
    fun `on passkey management click should display confirmation dialog and confirm click should emit PasskeyManagementClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Passkey management")
            .performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify { viewModel.trySendAction(AutoFillAction.PasskeyManagementClick) }
    }

    @Test
    fun `passkey management row should not appear according to state`() {
        mutableStateFlow.update {
            it.copy(
                showPasskeyManagementRow = false,
            )
        }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Passkey management").assertDoesNotExist()
    }

    @Test
    fun `use inline autofill should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(showInlineAutofillOption = true)
        }

        composeTestRule
            .onNodeWithText(text = "Use inline autofill")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(showInlineAutofillOption = false)
        }

        composeTestRule.onNodeWithText(text = "Use inline autofill").assertDoesNotExist()
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
            .onNodeWithContentDescription(label = "Default URI match detection.", substring = true)
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
            .onNodeWithContentDescription(label = "Default URI match detection.", substring = true)
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

    @Test
    fun `on default URI match type dialog cancel click should close the dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default URI match detection.", substring = true)
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
            .onNodeWithContentDescription(label = "Base domain", substring = true)
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription(label = "Starts with", substring = true)
            .assertDoesNotExist()
        mutableStateFlow.update {
            it.copy(defaultUriMatchType = UriMatchType.STARTS_WITH)
        }
        composeTestRule
            .onNodeWithContentDescription(label = "Base domain", substring = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription(label = "Starts with", substring = true)
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
            .onNodeWithText("Block autofill")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) }
    }

    @Test
    fun `on NavigateToBlockAutoFill should call onNavigateToBlockAutoFillScreen`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToBlockAutoFill)
        assertTrue(onNavigateToBlockAutoFillScreenCalled)
    }

    @Test
    fun `autofill action card should show when state is true and hide when false`() {
        composeTestRule
            .onNodeWithText("Get started")
            .assertDoesNotExist()
        mutableStateFlow.update { DEFAULT_STATE.copy(showAutofillActionCard = true) }
        composeTestRule
            .onNodeWithText("Get started")
            .assertIsDisplayed()
        mutableStateFlow.update { DEFAULT_STATE.copy(showAutofillActionCard = false) }
        composeTestRule
            .onNodeWithText("Get started")
            .assertDoesNotExist()
    }

    @Test
    fun `when autofill card is visible clicking the cta button should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(showAutofillActionCard = true) }
        composeTestRule
            .onNodeWithText("Get started")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(AutoFillAction.AutofillActionCardCtaClick) }
    }

    @Test
    fun `when autofill action card is visible clicking dismissing should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(showAutofillActionCard = true) }
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.DismissShowAutofillActionCard) }
    }

    @Test
    fun `when NavigateToSetupAutofill event is sent should call onNavigateToSetupAutofill`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToSetupAutofill)
        assertTrue(onNavigateToSetupAutoFillScreenCalled)
    }

    @Test
    fun `ChromeAutofillSettingsCard is only displayed when there are options in the list`() {
        val chromeAutofillSupportingText =
            "Improves login filling for supported websites on Chrome. " +
                "Once enabled, you’ll be directed to Chrome settings to enable " +
                "third-party autofill."

        composeTestRule
            .onNodeWithText(chromeAutofillSupportingText)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                chromeAutofillSettingsOptions = persistentListOf(
                    ChromeAutofillSettingsOption.Stable(enabled = true),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(chromeAutofillSupportingText)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when Chrome autofill options are clicked the correct action is sent`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                chromeAutofillSettingsOptions = persistentListOf(
                    ChromeAutofillSettingsOption.Stable(enabled = true),
                    ChromeAutofillSettingsOption.Beta(enabled = false),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Use Chrome autofill integration")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Use Chrome autofill integration (Beta)")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                AutoFillAction.ChromeAutofillSelected(ChromeReleaseChannel.BETA),
            )
            viewModel.trySendAction(
                AutoFillAction.ChromeAutofillSelected(ChromeReleaseChannel.STABLE),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when NavigateToChromeAutofillSettings events are sent they invoke the intent manager with the correct release channel`() {
        mutableEventFlow.tryEmit(
            AutoFillEvent.NavigateToChromeAutofillSettings(
                ChromeReleaseChannel.STABLE,
            ),
        )
        mutableEventFlow.tryEmit(
            AutoFillEvent.NavigateToChromeAutofillSettings(
                ChromeReleaseChannel.BETA,
            ),
        )

        verify(exactly = 1) {
            intentManager.startChromeAutofillSettingsActivity(ChromeReleaseChannel.BETA)
            intentManager.startChromeAutofillSettingsActivity(ChromeReleaseChannel.STABLE)
        }
    }
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAccessibilityAutofillEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    isUseInlineAutoFillEnabled = false,
    showInlineAutofillOption = true,
    showPasskeyManagementRow = true,
    defaultUriMatchType = UriMatchType.DOMAIN,
    showAutofillActionCard = false,
    activeUserId = "activeUserId",
    chromeAutofillSettingsOptions = persistentListOf(),
)
