package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class SetupAutofillScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<SetupAutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel = mockk<SetupAutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    private val intentManager = mockk<IntentManager>(relaxed = true)

    @Before
    fun setup() {
        composeTestRule.setContent {
            SetupAutoFillScreen(
                intentManager = intentManager,
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `Turning on autofill should send AutofillServiceChanged with value of true`() {
        composeTestRule
            .onNodeWithText("Autofill services")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(true))
        }
    }

    @Test
    fun `Turning off autofill should send AutofillServiceChanged with value of false`() {
        mutableStateFlow.update {
            it.copy(autofillEnabled = true)
        }
        composeTestRule
            .onNodeWithText("Autofill services", ignoreCase = true)
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(false))
        }
    }

    @Test
    fun `Continue click should send correct action`() {
        composeTestRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(SetupAutoFillAction.ContinueClick)
        }
    }

    @Test
    fun `Turn on later click should send correct action`() {
        composeTestRule
            .onNodeWithText("Turn on later")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        }
    }

    @Test
    fun `Turn on later component should not be displayed when not in initial setup`() {
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Turn on later")
            .assertDoesNotExist()
    }

    @Test
    fun `NavigateToAutoFillSettings should start system autofill settings activity`() {
        every { intentManager.startSystemAutofillSettingsActivity() } returns true
        mutableEventFlow.tryEmit(SetupAutoFillEvent.NavigateToAutofillSettings)
        verify {
            intentManager.startSystemAutofillSettingsActivity()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `NavigateToAutoFillSettings should send AutoFillServiceFallback action when intent fails`() {
        every { intentManager.startSystemAutofillSettingsActivity() } returns false
        mutableEventFlow.tryEmit(SetupAutoFillEvent.NavigateToAutofillSettings)
        verify { viewModel.trySendAction(SetupAutoFillAction.AutoFillServiceFallback) }
    }

    @Test
    fun `Show autofill fallback dialog when dialog state is AutoFillFallbackDialog`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = SetupAutoFillDialogState.AutoFillFallbackDialog,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "We were unable to automatically open the Android autofill",
                substring = true,
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `When autofill fallback dialog is dismissed, sends action to dismiss dialog and is removed when state is null`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = SetupAutoFillDialogState.AutoFillFallbackDialog,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(SetupAutoFillAction.DismissDialog) }
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `Show turn on later dialog when dialog state is TurnOnLaterDialog`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = SetupAutoFillDialogState.TurnOnLaterDialog,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Turn on autofill later?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `On confirm click on TurnOnLaterDialog, sends action to turn on later`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = SetupAutoFillDialogState.TurnOnLaterDialog,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterConfirmClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `When turn on later dialog is dismissed, sends action to dismiss dialog and is removed when state is null`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = SetupAutoFillDialogState.TurnOnLaterDialog,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(SetupAutoFillAction.DismissDialog) }
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(SetupAutoFillEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `close icon should not show when in initial setup`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertDoesNotExist()
    }

    @Test
    fun `close icon should show when not initial setup and send action when clicked`() {
        mutableStateFlow.update { it.copy(isInitialSetup = false) }
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertIsDisplayed()
            .performClick()

        verify { viewModel.trySendAction(SetupAutoFillAction.CloseClick) }
    }
}

private val DEFAULT_STATE = SetupAutoFillState(
    userId = "userId",
    dialogState = null,
    autofillEnabled = false,
    isInitialSetup = true,
)
