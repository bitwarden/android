package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlightRecorderViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick action should send the NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FlightRecorderAction.BackClick)
            assertEquals(FlightRecorderEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on SaveClick action should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FlightRecorderAction.SaveClick)
            expectNoEvents()
        }
    }

    @Test
    fun `on HelpCenterClick action should send the NavigateToHelpCenter event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FlightRecorderAction.HelpCenterClick)
            assertEquals(FlightRecorderEvent.NavigateToHelpCenter, awaitItem())
        }
    }

    @Test
    fun `on DurationSelect action should update the selectedDuration state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                FlightRecorderAction.DurationSelect(duration = FlightRecorderDuration.ONE_WEEK),
            )
            assertEquals(
                DEFAULT_STATE.copy(selectedDuration = FlightRecorderDuration.ONE_WEEK),
                awaitItem(),
            )

            viewModel.trySendAction(
                FlightRecorderAction.DurationSelect(duration = FlightRecorderDuration.EIGHT_HOURS),
            )
            assertEquals(
                DEFAULT_STATE.copy(selectedDuration = FlightRecorderDuration.EIGHT_HOURS),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: FlightRecorderState? = null,
    ): FlightRecorderViewModel =
        FlightRecorderViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
            },
        )
}

private val DEFAULT_STATE: FlightRecorderState =
    FlightRecorderState(
        selectedDuration = FlightRecorderDuration.ONE_HOUR,
    )
