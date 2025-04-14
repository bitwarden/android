package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util.toViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RecordedLogsViewModelTest : BaseViewModelTest() {

    private val fileManager = mockk<FileManager> {
        every { logsDirectory } returns "/logs"
    }
    private val mutableFlightRecorderDataFlow = MutableStateFlow(FlightRecorderDataSet(emptySet()))
    private val settingsRepository = mockk<SettingsRepository> {
        every { flightRecorderDataFlow } returns mutableFlightRecorderDataFlow
        every { deleteAllLogs() } just runs
        every { deleteLog(data = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(FlightRecorderDataSet::toViewState)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FlightRecorderDataSet::toViewState)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on flightRecorderDataFlow emission should update the viewState`() {
        val viewState = RecordedLogsState.ViewState.Content(items = persistentListOf())
        val dataset = mockk<FlightRecorderDataSet> {
            every { toViewState(clock = FIXED_CLOCK, logsFolder = "/logs") } returns viewState
        }
        val viewModel = createViewModel()

        mutableFlightRecorderDataFlow.value = dataset

        verify(exactly = 1) {
            dataset.toViewState(clock = FIXED_CLOCK, logsFolder = "/logs")
        }
        assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick action should send the NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(RecordedLogsAction.BackClick)
            assertEquals(RecordedLogsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on ShareAllClick action should do nothing`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RecordedLogsAction.ShareAllClick)
    }

    @Test
    fun `on ShareClick action should do nothing`() {
        val viewModel = createViewModel()
        val item = mockk<RecordedLogsState.DisplayItem>()
        viewModel.trySendAction(RecordedLogsAction.ShareClick(item = item))
    }

    @Test
    fun `on DeleteAllClick action should call deleteAllLogs`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RecordedLogsAction.DeleteAllClick)
        verify(exactly = 1) {
            settingsRepository.deleteAllLogs()
        }
    }

    @Test
    fun `on DeleteClick action should call deleteLog`() {
        val data = mockk<FlightRecorderDataSet.FlightRecorderData> {
            every { id } returns "50"
        }
        val dataset = FlightRecorderDataSet(data = setOf(data))
        every { settingsRepository.flightRecorderData } returns dataset
        val viewModel = createViewModel()
        val item = mockk<RecordedLogsState.DisplayItem> {
            every { id } returns "50"
        }
        viewModel.trySendAction(RecordedLogsAction.DeleteClick(item = item))
        verify(exactly = 1) {
            settingsRepository.deleteLog(data = data)
        }
    }

    private fun createViewModel(
        state: RecordedLogsState? = null,
    ): RecordedLogsViewModel =
        RecordedLogsViewModel(
            clock = FIXED_CLOCK,
            fileManager = fileManager,
            settingsRepository = settingsRepository,
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
            },
        )
}

private val DEFAULT_STATE: RecordedLogsState =
    RecordedLogsState(
        viewState = RecordedLogsState.ViewState.Empty,
        logsFolder = "/logs",
    )

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
