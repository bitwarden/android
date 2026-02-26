package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
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

class SetupBrowserAutofillScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled = false
    private val intentManager = mockk<IntentManager> {
        every { launchUri(uri = any()) } just runs
    }

    private val mutableEventFlow = bufferedMutableSharedFlow<SetupBrowserAutofillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<SetupBrowserAutofillViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        mockkStatic(IntentManager::startBrowserAutofillSettingsActivity)
        setContent(
            intentManager = intentManager,
        ) {
            SetupBrowserAutofillScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(IntentManager::startBrowserAutofillSettingsActivity)
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SetupBrowserAutofillEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToBrowserIntegrationsInfo should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SetupBrowserAutofillEvent.NavigateToBrowserIntegrationsInfo)
        verify(exactly = 1) {
            intentManager.launchUri(
                uri = "https://bitwarden.com/help/auto-fill-android/#browser-integrations/".toUri(),
            )
        }
    }

    @Test
    fun `NavigateToBrowserAutofillSettings should start system autofill settings activity`() {
        val browserPackage = BrowserPackage.CHROME_STABLE
        every { intentManager.startBrowserAutofillSettingsActivity(browserPackage) } returns true
        mutableEventFlow.tryEmit(
            value = SetupBrowserAutofillEvent.NavigateToBrowserAutofillSettings(browserPackage),
        )
        verify(exactly = 1) {
            intentManager.startBrowserAutofillSettingsActivity(browserPackage)
        }
    }

    @Test
    fun `BrowserIntegrationClick should emit when integration row is clicked`() {
        composeTestRule
            .onNodeWithText(text = "Use Brave autofill integration")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                SetupBrowserAutofillAction.BrowserIntegrationClick(BrowserPackage.BRAVE_RELEASE),
            )
        }
    }

    @Test
    fun `appbar title is updated according to state`() {
        mutableStateFlow.update { it.copy(isInitialSetup = true) }
        composeTestRule.onNodeWithText(text = "Account setup").assertExists()
        composeTestRule.onNodeWithText(text = "Autofill setup").assertDoesNotExist()
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule.onNodeWithText(text = "Account setup").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "Autofill setup").assertExists()
    }

    @Test
    fun `close button is updated according to state`() {
        mutableStateFlow.update { it.copy(isInitialSetup = true) }
        composeTestRule.onNodeWithContentDescription(label = "Close").assertDoesNotExist()
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule.onNodeWithContentDescription(label = "Close").assertExists()
    }

    @Test
    fun `why is this step required button click should emit WhyIsThisStepRequiredClick`() {
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule
            .onNodeWithText(text = "Why is this step required?")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.WhyIsThisStepRequiredClick)
        }
    }

    @Test
    fun `close button click should emit CloseClick`() {
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.CloseClick)
        }
    }

    @Test
    fun `turn on later button is updated according to state`() {
        mutableStateFlow.update { it.copy(isInitialSetup = true) }
        composeTestRule.onNodeWithText(text = "Turn on later").assertExists()
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule.onNodeWithText(text = "Turn on later").assertDoesNotExist()
    }

    @Test
    fun `continue button is enabled or disabled according to state`() {
        composeTestRule
            .onNodeWithText(text = "Continue")
            .assertIsEnabled()
        mutableStateFlow.update {
            it.copy(
                browserAutofillSettingsOptions = persistentListOf(
                    BrowserAutofillSettingsOption.BraveStable(enabled = false),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "Continue")
            .assertIsNotEnabled()
    }

    @Test
    fun `ContinueClick should emit when enabled and clicked`() {
        composeTestRule
            .onNodeWithText(text = "Continue")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.ContinueClick)
        }
    }

    @Test
    fun `TurnOnLaterClick should emit when clicked`() {
        composeTestRule
            .onNodeWithText(text = "Turn on later")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterClick)
        }
    }

    @Test
    fun `correct dialog should be displayed according to state`() {
        composeTestRule.assertNoDialogExists()
        mutableStateFlow.update {
            it.copy(dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog)
        }

        composeTestRule.onNode(isDialog()).assertExists()

        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `DismissDialog should emit when dialog is dismissed`() {
        mutableStateFlow.update {
            it.copy(dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog)
        }

        composeTestRule
            .onNodeWithText(text = "Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.DismissDialog)
        }
    }

    @Test
    fun `TurnOnLaterConfirmClick should emit when dialog is confirmed`() {
        mutableStateFlow.update {
            it.copy(dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog)
        }

        composeTestRule
            .onNodeWithText(text = "Confirm")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterConfirmClick)
        }
    }
}

private val DEFAULT_STATE: SetupBrowserAutofillState = SetupBrowserAutofillState(
    dialogState = null,
    isInitialSetup = true,
    browserAutofillSettingsOptions = persistentListOf(
        BrowserAutofillSettingsOption.BraveStable(enabled = true),
    ),
)
