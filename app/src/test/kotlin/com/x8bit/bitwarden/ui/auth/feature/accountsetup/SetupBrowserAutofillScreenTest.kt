package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import com.x8bit.bitwarden.ui.platform.manager.utils.startBrowserAutofillSettingsActivity
import com.x8bit.bitwarden.ui.platform.manager.utils.startSystemAutofillSettingsActivity
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
import org.junit.Before
import org.junit.Test

class SetupBrowserAutofillScreenTest : BitwardenComposeTest() {
    private val intentManager = mockk<IntentManager>()

    private val mutableEventFlow = bufferedMutableSharedFlow<SetupBrowserAutofillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<SetupBrowserAutofillViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        mockkStatic(IntentManager::startSystemAutofillSettingsActivity)
        setContent(
            intentManager = intentManager,
        ) {
            SetupBrowserAutofillScreen(
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(IntentManager::startSystemAutofillSettingsActivity)
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
    browserAutofillSettingsOptions = persistentListOf(
        BrowserAutofillSettingsOption.BraveStable(enabled = true),
    ),
)
