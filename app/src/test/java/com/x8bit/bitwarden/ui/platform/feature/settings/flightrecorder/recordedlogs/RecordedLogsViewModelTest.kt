package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecordedLogsViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick action should send the NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(RecordedLogsAction.BackClick)
            assertEquals(RecordedLogsEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: RecordedLogsState? = null,
    ): RecordedLogsViewModel =
        RecordedLogsViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
            },
        )
}

private val DEFAULT_STATE: RecordedLogsState =
    RecordedLogsState(
        viewState = RecordedLogsState.ViewState.Loading,
    )
