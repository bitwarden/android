package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startSystemAccessibilitySettingsActivity
import com.bitwarden.ui.platform.manager.util.startSystemAutofillSettingsActivity
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import com.x8bit.bitwarden.ui.platform.manager.utils.startBrowserAutofillSettingsActivity
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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class AutoFillScreenTest : BitwardenComposeTest() {

    private var isSystemSettingsRequestSuccess = false
    private var onNavigateBackCalled = false
    private var onNavigateToBlockAutoFillScreenCalled = false
    private var onNavigateToSetupAutoFillScreenCalled = false
    private var onNavigateToSetupBrowserAutofillScreenCalled = false
    private var onNavigateToAboutPrivilegedAppsScreenCalled = false
    private var onNavigateToPrivilegedAppsListCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<AutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { startCredentialManagerSettings() } just runs
        every { launchUri(any()) } just runs
    }

    @Before
    fun setUp() {
        mockkStatic(
            IntentManager::startSystemAutofillSettingsActivity,
            IntentManager::startSystemAccessibilitySettingsActivity,
            IntentManager::startBrowserAutofillSettingsActivity,
        )
        every { intentManager.startBrowserAutofillSettingsActivity(any()) } returns true
        every {
            intentManager.startSystemAutofillSettingsActivity()
        } answers { isSystemSettingsRequestSuccess }
        every { intentManager.startSystemAccessibilitySettingsActivity() } returns true

        setContent(
            intentManager = intentManager,
        ) {
            AutoFillScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToBlockAutoFillScreen = { onNavigateToBlockAutoFillScreenCalled = true },
                onNavigateToSetupAutofill = { onNavigateToSetupAutoFillScreenCalled = true },
                onNavigateToSetupBrowserAutofill = {
                    onNavigateToSetupBrowserAutofillScreenCalled = true
                },
                onNavigateToAboutPrivilegedAppsScreen = {
                    onNavigateToAboutPrivilegedAppsScreenCalled = true
                },
                onNavigateToPrivilegedAppsList = {
                    onNavigateToPrivilegedAppsListCalled = true
                },
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(
            IntentManager::startSystemAutofillSettingsActivity,
            IntentManager::startSystemAccessibilitySettingsActivity,
            IntentManager::startBrowserAutofillSettingsActivity,
        )
    }

    @Test
    fun `on NavigateToAutofillHelp should launch the browser to the autofill help page`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAutofillHelp)
        verify(exactly = 1) {
            intentManager.launchUri(
                uri = "https://bitwarden.com/help/auto-fill-android-troubleshooting/".toUri(),
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
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on NavigateToSettings should attempt to navigate to credential manager settings`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToSettings)

        verify { intentManager.startCredentialManagerSettings() }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on help card CTA should send HelpCardClick`() {
        composeTestRule
            .onNodeWithText(text = "Having trouble with autofill?")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AutoFillAction.HelpCardClick)
        }
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
            .onAllNodesWithText(text = "Okay")
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
    fun `on inline autofill style selected should send AutofillStyleSelected`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                autofillStyle = AutofillStyle.POPUP,
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                label = "Popup (shows over input field). Display autofill suggestions",
            )
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Inline (shows in keyboard)")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(AutoFillAction.AutofillStyleSelected(AutofillStyle.INLINE))
        }
    }

    @Test
    fun `autofill style should be display selection according to state`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                autofillStyle = AutofillStyle.INLINE,
            )
        }
        composeTestRule
            .onNodeWithContentDescription(
                label = "Inline (shows in keyboard). Display autofill suggestions",
            )
            .performScrollTo()
            .assertIsDisplayed()
        mutableStateFlow.update { it.copy(autofillStyle = AutofillStyle.POPUP) }
        composeTestRule
            .onNodeWithContentDescription(
                label = "Popup (shows over input field). Display autofill suggestions",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `use display autofill suggestions should be visible or enabled according to state`() {
        mutableStateFlow.update {
            it.copy(isAutoFillServicesEnabled = true)
        }

        composeTestRule
            .onNodeWithContentDescription(
                label = "Inline (shows in keyboard). Display autofill suggestions",
            )
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(isAutoFillServicesEnabled = false)
        }

        composeTestRule
            .onNodeWithContentDescription(
                label = "Inline (shows in keyboard). Display autofill suggestions",
            )
            .assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on passkey management click should display confirmation dialog and confirm click should emit PasskeyManagementClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Passkey management")
            .performScrollTo()
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
            it.copy(
                isAutoFillServicesEnabled = true,
                showInlineAutofillOption = true,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                label = "Inline (shows in keyboard). Display autofill suggestions",
            )
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(showInlineAutofillOption = false)
        }

        composeTestRule
            .onNodeWithContentDescription(
                label = "Inline (shows in keyboard). Display autofill suggestions",
            )
            .assertDoesNotExist()
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
    fun `on Ask to add item toggle should send AskToAddLoginClick`() {
        composeTestRule
            .onNodeWithText("Ask to add item")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(true)) }
    }

    @Test
    fun `Ask to add item should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Ask to add item")
            .performScrollTo()
            .assertIsOff()
        mutableStateFlow.update { it.copy(isAskToAddLoginEnabled = true) }
        composeTestRule
            .onNodeWithText("Ask to add item")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `on default URI match type click should display dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Default URI match detection")
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
            .onNodeWithText(text = "Default URI match detection")
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
            .onNodeWithText(text = "Default URI match detection")
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
    fun `browser autofill action card should show when state is true and hide when false`() {
        composeTestRule
            .onNodeWithText(text = "Get started")
            .assertDoesNotExist()
        mutableStateFlow.update { DEFAULT_STATE.copy(showBrowserAutofillActionCard = true) }
        composeTestRule
            .onNodeWithText(text = "Get started")
            .assertIsDisplayed()
        mutableStateFlow.update { DEFAULT_STATE.copy(showBrowserAutofillActionCard = false) }
        composeTestRule
            .onNodeWithText(text = "Get started")
            .assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when browser autofill card is visible clicking the cta button should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(showBrowserAutofillActionCard = true) }
        composeTestRule
            .onNodeWithText(text = "Get started")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AutoFillAction.BrowserAutofillActionCardCtaClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when browser autofill action card is visible clicking dismissing should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(showBrowserAutofillActionCard = true) }
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AutoFillAction.DismissShowBrowserAutofillActionCard)
        }
    }

    @Test
    fun `when NavigateToSetupAutofill event is sent should call onNavigateToSetupAutofill`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToSetupAutofill)
        assertTrue(onNavigateToSetupAutoFillScreenCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when NavigateToSetupBrowserAutofill event is sent should call onNavigateToSetupBrowserAutofill`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToSetupBrowserAutofill)
        assertTrue(onNavigateToSetupBrowserAutofillScreenCalled)
    }

    @Test
    fun `BrowserAutofillSettingsCard is only displayed when there are options in the list`() {
        mutableStateFlow.update { it.copy(isAutoFillServicesEnabled = true) }
        val browserAutofillSupportingText =
            "Improves login filling for supported websites on selected browsers. " +
                "Once enabled, you’ll be directed to browser settings to enable " +
                "third-party autofill."

        composeTestRule
            .onNodeWithText(browserAutofillSupportingText)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                browserAutofillSettingsOptions = persistentListOf(
                    BrowserAutofillSettingsOption.ChromeStable(enabled = true),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(browserAutofillSupportingText)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when browser autofill options are clicked the correct action is sent`() {
        mutableStateFlow.update {
            it.copy(
                isAutoFillServicesEnabled = true,
                browserAutofillSettingsOptions = persistentListOf(
                    BrowserAutofillSettingsOption.ChromeStable(enabled = true),
                    BrowserAutofillSettingsOption.ChromeBeta(enabled = false),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Use Chrome autofill integration")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Use Chrome Beta autofill integration")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                AutoFillAction.BrowserAutofillSelected(BrowserPackage.CHROME_BETA),
            )
            viewModel.trySendAction(
                AutoFillAction.BrowserAutofillSelected(BrowserPackage.CHROME_STABLE),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when NavigateToBrowserAutofillSettings events are sent they invoke the intent manager with the correct release channel`() {
        mutableEventFlow.tryEmit(
            AutoFillEvent.NavigateToBrowserAutofillSettings(
                BrowserPackage.CHROME_STABLE,
            ),
        )
        mutableEventFlow.tryEmit(
            AutoFillEvent.NavigateToBrowserAutofillSettings(
                BrowserPackage.CHROME_BETA,
            ),
        )

        verify(exactly = 1) {
            intentManager.startBrowserAutofillSettingsActivity(BrowserPackage.CHROME_BETA)
            intentManager.startBrowserAutofillSettingsActivity(BrowserPackage.CHROME_STABLE)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `NavigateToAboutPrivilegedAppsScreen event should call onNavigateToAboutPrivilegedAppsScreen`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToAboutPrivilegedAppsScreen)
        assertTrue(onNavigateToAboutPrivilegedAppsScreenCalled)
    }

    @Test
    fun `privileged app help link click should send AboutPrivilegedAppsClick`() {
        composeTestRule
            .onNodeWithContentDescription("Learn more about privileged apps")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(AutoFillAction.AboutPrivilegedAppsClick)
        }
    }

    @Test
    fun `on NavigateToPrivilegedAppsList should call onNavigateToPrivilegedAppsList`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToPrivilegedAppsListScreen)
        assertTrue(onNavigateToPrivilegedAppsListCalled)
    }

    @Test
    fun `privileged apps row click should send PrivilegedAppsClick`() {
        composeTestRule
            .onNodeWithText("Privileged apps")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(AutoFillAction.PrivilegedAppsClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on default URI match type dialog item click should send warning when is an Advanced Option`() {
        composeTestRule
            .onNodeWithText(text = "Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Starts with")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(
                "“Starts with” is an advanced option with " +
                    "increased risk of exposing credentials.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on advanced match detection warning dialog click on cancel should not change the default URI match type`() {
        composeTestRule
            .onNodeWithText(text = "Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Starts with")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                AutoFillAction.DefaultUriMatchTypeSelect(
                    defaultUriMatchType = UriMatchType.STARTS_WITH,
                ),
            )
        }
    }

    @Test
    fun `on Advanced matching warning dialog confirm should display learn more dialog`() {
        composeTestRule
            .onNodeWithText(text = "Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Starts with")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Keep your credentials secure")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Advanced matching warning dialog click on more about match detection should call launchUri`() {
        composeTestRule
            .onNodeWithText(text = "Default URI match detection")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Starts with")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Learn more")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                AutoFillAction.LearnMoreClick,
            )
        }
    }

    @Test
    fun `on NavigateToLearnMore should call launchUri`() {
        mutableEventFlow.tryEmit(AutoFillEvent.NavigateToLearnMore)
        verify(exactly = 1) {
            intentManager.launchUri("https://bitwarden.com/help/uri-match-detection/".toUri())
        }
    }
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAccessibilityAutofillEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    autofillStyle = AutofillStyle.INLINE,
    showInlineAutofillOption = true,
    showPasskeyManagementRow = true,
    defaultUriMatchType = UriMatchType.DOMAIN,
    showAutofillActionCard = false,
    showBrowserAutofillActionCard = false,
    activeUserId = "activeUserId",
    browserAutofillSettingsOptions = persistentListOf(),
)
