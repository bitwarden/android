package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsScreen
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
}

private val DEFAULT_STATE: RecordedLogsState = RecordedLogsState
