package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
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

class RecordedLogsScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<RecordedLogsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val intentManager = mockk<IntentManager> {
        every { shareFile(title = any(), fileUri = any()) } just runs
    }

    private val viewModel = mockk<RecordedLogsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
        ) {
            RecordedLogsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on navigation icon click should emit BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.BackClick)
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(RecordedLogsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on ShowSnackbar event should display the snackbar with the correct message`() {
        val message = "Test Snackbar Message"
        mutableEventFlow.tryEmit(RecordedLogsEvent.ShowSnackbar(text = message.asText()))

        composeTestRule
            .onNodeWithText(text = message)
            .assertIsDisplayed()
    }

    @Test
    fun `on ShareLog event should invoke shareFile on intent manager`() {
        val stringUri = "/logs"
        mutableEventFlow.tryEmit(RecordedLogsEvent.ShareLog(uri = stringUri))
        verify {
            intentManager.shareFile(title = null, fileUri = stringUri.toUri())
        }
    }

    @Test
    fun `UI should change based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = RecordedLogsState.ViewState.Loading) }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = RecordedLogsState.ViewState.Empty) }
        composeTestRule.onNodeWithText(text = "No logs recorded").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = RecordedLogsState.ViewState.Content(
                    items = persistentListOf(
                        RecordedLogsState.DisplayItem(
                            id = "50",
                            title = "2025-04-12T03:15:52 – 2025-04-12T04:15:52".asText(),
                            subtextStart = "1.00 KB".asText(),
                            subtextEnd = null,
                            isDeletedEnabled = false,
                        ),
                        RecordedLogsState.DisplayItem(
                            id = "52",
                            title = "2025-04-12T03:15:00 – 2025-04-12T04:15:00".asText(),
                            subtextStart = "1.00 KB".asText(),
                            subtextEnd = BitwardenString.expires_on.asText("4/12/25"),
                            isDeletedEnabled = true,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "2025-04-12T03:15:52 – 2025-04-12T04:15:52")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "2025-04-12T03:15:00 – 2025-04-12T04:15:00")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `on Share All click should emit ShareAllClick`() {
        mutableStateFlow.update {
            it.copy(viewState = RecordedLogsState.ViewState.Content(items = persistentListOf()))
        }
        composeTestRule.onNodeWithContentDescription(label = "More").performClick()
        composeTestRule.onNodeWithText(text = "Share all").performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.ShareAllClick)
        }
    }

    @Test
    fun `on Delete All click should display Dialog, on Yes click emits DeleteAllClick`() {
        mutableStateFlow.update {
            it.copy(viewState = RecordedLogsState.ViewState.Content(items = persistentListOf()))
        }
        composeTestRule.onNodeWithContentDescription(label = "More").performClick()
        composeTestRule.onNodeWithText(text = "Delete all").performClick()
        composeTestRule
            .onAllNodesWithText(text = "Delete logs")
            .filterToOne(matcher = hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Yes")
            .filterToOne(matcher = hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.DeleteAllClick)
        }
    }

    @Test
    fun `individual delete button should be enabled based on state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = RecordedLogsState.ViewState.Content(
                    items = persistentListOf(
                        RecordedLogsState.DisplayItem(
                            id = "50",
                            title = "title".asText(),
                            subtextStart = "1.00 KB".asText(),
                            subtextEnd = null,
                            isDeletedEnabled = true,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "title")
            .performScrollTo()
            .onSiblings()
            .filterToOne(matcher = hasContentDescription(value = "More"))
            .performClick()
        composeTestRule.onNodeWithText(text = "Delete").assertIsEnabled()

        mutableStateFlow.update {
            it.copy(
                viewState = RecordedLogsState.ViewState.Content(
                    items = persistentListOf(
                        RecordedLogsState.DisplayItem(
                            id = "50",
                            title = "title".asText(),
                            subtextStart = "1.00 KB".asText(),
                            subtextEnd = null,
                            isDeletedEnabled = false,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "title")
            .performScrollTo()
            .onSiblings()
            .filterToOne(matcher = hasContentDescription(value = "More"))
            .performClick()
        composeTestRule.onNodeWithText(text = "Delete").assertIsNotEnabled()
    }

    @Test
    fun `on individual Share click should emit ShareClick`() {
        val displayItem = RecordedLogsState.DisplayItem(
            id = "50",
            title = "title".asText(),
            subtextStart = "1.00 KB".asText(),
            subtextEnd = null,
            isDeletedEnabled = true,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = RecordedLogsState.ViewState.Content(
                    items = persistentListOf(displayItem),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "title")
            .performScrollTo()
            .onSiblings()
            .filterToOne(matcher = hasContentDescription(value = "More"))
            .performClick()
        composeTestRule.onNodeWithText(text = "Share").performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.ShareClick(displayItem))
        }
    }

    @Test
    fun `on individual Delete click should display Dialog, on Yes click emits DeleteClick`() {
        val displayItem = RecordedLogsState.DisplayItem(
            id = "50",
            title = "title".asText(),
            subtextStart = "1.00 KB".asText(),
            subtextEnd = null,
            isDeletedEnabled = true,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = RecordedLogsState.ViewState.Content(
                    items = persistentListOf(displayItem),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "title")
            .performScrollTo()
            .onSiblings()
            .filterToOne(matcher = hasContentDescription(value = "More"))
            .performClick()
        composeTestRule.onNodeWithText(text = "Delete").performClick()
        composeTestRule
            .onAllNodesWithText(text = "Delete log")
            .filterToOne(matcher = hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Yes")
            .filterToOne(matcher = hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.DeleteClick(displayItem))
        }
    }

    @Test
    fun `dialog should update according to state`() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = RecordedLogsState.DialogState.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                    error = null,
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText(text = "title")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }
}

private val DEFAULT_STATE: RecordedLogsState =
    RecordedLogsState(
        viewState = RecordedLogsState.ViewState.Loading,
        dialogState = null,
        logsFolder = "/logs",
    )
