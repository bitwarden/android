package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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

    private fun createViewModel(
        state: FlightRecorderState? = null,
    ): FlightRecorderViewModel =
        FlightRecorderViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
            },
        )
}

private val DEFAULT_STATE: FlightRecorderState = FlightRecorderState
