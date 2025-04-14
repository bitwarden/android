package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordedLogsScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<RecordedLogsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel = mockk<RecordedLogsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent {
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
    fun `UI should change based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = RecordedLogsState.ViewState.Loading) }
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

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
                            subtextEnd = R.string.expires_in_days.asText("30"),
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
    fun `on Delete All click should emit DeleteAllClick`() {
        mutableStateFlow.update {
            it.copy(viewState = RecordedLogsState.ViewState.Content(items = persistentListOf()))
        }
        composeTestRule.onNodeWithContentDescription(label = "More").performClick()
        composeTestRule.onNodeWithText(text = "Delete all").performClick()

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
    fun `on individual Delete click should emit DeleteClick`() {
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

        verify(exactly = 1) {
            viewModel.trySendAction(RecordedLogsAction.DeleteClick(displayItem))
        }
    }
}

private val DEFAULT_STATE: RecordedLogsState =
    RecordedLogsState(
        viewState = RecordedLogsState.ViewState.Loading,
        logsFolder = "/logs",
    )
