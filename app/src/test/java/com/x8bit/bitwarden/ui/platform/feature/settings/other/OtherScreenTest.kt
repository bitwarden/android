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
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtherScreenTest : BaseComposeTest() {

    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<OtherEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<OtherViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            OtherScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
    }

    @Test
    fun `on allow screen capture confirm should send AllowScreenCaptureToggle`() {
        composeTestRule.onNodeWithText("Allow screen capture").performClick()
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.assertNoDialogExists()

        verify { viewModel.trySendAction(OtherAction.AllowScreenCaptureToggle(true)) }
    }

    @Test
    fun `on allow screen capture cancel should dismiss dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on allow screen capture row click should display confirm enable screen capture dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performClick()
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
    fun `on clear clipboard row click should show show clipboard selection dialog`() {
        composeTestRule.onNodeWithText("Clear clipboard").performClick()
        composeTestRule
            .onAllNodesWithText("Clear clipboard")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on clear clipboard dialog item click should send ClearClipboardFrequencyChange`() {
        composeTestRule.onNodeWithText("Clear clipboard").performClick()
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
        composeTestRule.onNodeWithText("Clear clipboard").performClick()
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
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = OtherState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }
}

private val DEFAULT_STATE = OtherState(
    allowScreenCapture = false,
    allowSyncOnRefresh = false,
    clearClipboardFrequency = ClearClipboardFrequency.NEVER,
    lastSyncTime = "5/14/2023 4:52 PM",
    dialogState = null,
)
