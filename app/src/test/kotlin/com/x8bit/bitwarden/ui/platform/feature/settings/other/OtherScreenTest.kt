package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtherScreenTest : BitwardenComposeTest() {

    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<OtherEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<OtherViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            OtherScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
    }

    @Test
    fun `on allow screen capture confirm should send AllowScreenCaptureToggle`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.assertNoDialogExists()

        verify { viewModel.trySendAction(OtherAction.AllowScreenCaptureToggle(true)) }
    }

    @Test
    fun `on allow screen capture cancel should dismiss dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on allow screen capture row click should display confirm enable screen capture dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule
            .onAllNodesWithText("Allow screen capture")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on allow sync toggle should send AllowSyncToggle`() {
        composeTestRule.onNodeWithText("Allow sync on refresh").performClick()
        verify { viewModel.trySendAction(OtherAction.AllowSyncToggle(true)) }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(OtherAction.BackClick) }
    }

    @Test
    fun `on ShowSnackbar should display snackbar content`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        composeTestRule.onNodeWithText(text = message).assertDoesNotExist()
        mutableEventFlow.tryEmit(OtherEvent.ShowSnackbar(data = data))
        composeTestRule.onNodeWithText(text = message).assertIsDisplayed()
    }

    @Test
    fun `on clear clipboard row click should show show clipboard selection dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Never. Clear clipboard")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Clear clipboard")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on clear clipboard dialog item click should send ClearClipboardFrequencyChange`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Never. Clear clipboard")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("10 seconds")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                OtherAction.ClearClipboardFrequencyChange(
                    clearClipboardFrequency = ClearClipboardFrequency.TEN_SECONDS,
                ),
            )
        }
    }

    @Test
    fun `on clear clipboard dialog cancel should dismiss dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Never. Clear clipboard")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on sync now button click should send SyncNowButtonClick`() {
        composeTestRule.onNodeWithText("Sync now").performClick()
        verify { viewModel.trySendAction(OtherAction.SyncNowButtonClick) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(OtherEvent.NavigateBack)
        assertTrue(haveCalledNavigateBack)
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = OtherState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `should display correct items according to state`() {
        mutableStateFlow.update { it.copy(isPreAuth = false) }
        composeTestRule
            .onNodeWithText(text = "Allow sync on refresh")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Sync now")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Clear clipboard", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Allow screen capture")
            .assertExists()

        mutableStateFlow.update { it.copy(isPreAuth = true) }
        composeTestRule
            .onNodeWithText(text = "Allow sync on refresh")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Sync now")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Clear clipboard", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Allow screen capture")
            .assertExists()
    }
}

private val DEFAULT_STATE = OtherState(
    isPreAuth = false,
    allowScreenCapture = false,
    allowSyncOnRefresh = false,
    clearClipboardFrequency = ClearClipboardFrequency.NEVER,
    lastSyncTime = "5/14/2023 4:52 PM",
    dialogState = null,
)
