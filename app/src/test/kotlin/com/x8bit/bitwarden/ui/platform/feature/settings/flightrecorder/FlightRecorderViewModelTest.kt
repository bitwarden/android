package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.data.manager.model.FlightRecorderDuration
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlightRecorderViewModelTest : BaseViewModelTest() {

    private val settingsRepository = mockk<SettingsRepository> {
        every { startFlightRecorder(duration = any()) } just runs
    }

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
    fun `on SaveClick action should start the flight recorder and send the NavigateBack event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(FlightRecorderAction.SaveClick)
                assertEquals(FlightRecorderEvent.NavigateBack, awaitItem())
            }

            verify(exactly = 1) {
                settingsRepository.startFlightRecorder(
                    duration = FlightRecorderDuration.TWENTY_FOUR_HOURS,
                )
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
            settingsRepository = settingsRepository,
        )
}

private val DEFAULT_STATE: FlightRecorderState =
    FlightRecorderState(
        selectedDuration = FlightRecorderDuration.TWENTY_FOUR_HOURS,
    )
