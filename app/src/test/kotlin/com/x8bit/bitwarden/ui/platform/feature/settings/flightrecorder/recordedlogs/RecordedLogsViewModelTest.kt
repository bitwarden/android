package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.manager.model.ZipFileResult
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsAction
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsEvent
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util.toViewState
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.io.File
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
        every { flightRecorderData } returns FlightRecorderDataSet(emptySet())
        every { deleteAllLogs() } just runs
        every { deleteLog(data = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            FlightRecorderDataSet::toViewState,
            File::toURI,
            Uri::parse,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            FlightRecorderDataSet::toViewState,
            File::toURI,
            Uri::parse,
        )
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
    fun `on ShareAllClick action with zipUriToCache Success should send ShareLog event`() =
        runTest {
            val uri = setupMockUri("/logs")
            val zipFile = mockk<File>()
            coEvery { fileManager.zipUriToCache(uri = uri) } returns ZipFileResult.Success(zipFile)
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(RecordedLogsAction.ShareAllClick)
                assertEquals(RecordedLogsEvent.ShareLog(uri = zipFile.toString()), awaitItem())
            }

            coVerify(exactly = 1) {
                fileManager.zipUriToCache(uri = uri)
            }
        }

    @Test
    fun `on ShareAllClick action with zipUriToCache Failure should display error dialog`() {
        val uri = setupMockUri("/logs")
        val error = Throwable("Fail!")
        coEvery { fileManager.zipUriToCache(uri = uri) } returns ZipFileResult.Failure(error)
        val viewModel = createViewModel(DEFAULT_STATE)

        viewModel.trySendAction(RecordedLogsAction.ShareAllClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = RecordedLogsState.DialogState.Error(
                    title = BitwardenString.unable_to_share.asText(),
                    message = BitwardenString.please_try_again_or_select_a_different_log.asText(),
                    error = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            fileManager.zipUriToCache(uri = uri)
        }
    }

    @Test
    fun `on ShareAllClick action with zipUriToCache NothingToZip should display error dialog`() {
        val uri = setupMockUri("/logs")
        coEvery { fileManager.zipUriToCache(uri = uri) } returns ZipFileResult.NothingToZip
        val viewModel = createViewModel(DEFAULT_STATE)

        viewModel.trySendAction(RecordedLogsAction.ShareAllClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = RecordedLogsState.DialogState.Error(
                    title = BitwardenString.unable_to_share.asText(),
                    message = BitwardenString.the_log_file_you_are_trying_to_share_has_been_removed
                        .asText(),
                    error = null,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            fileManager.zipUriToCache(uri = uri)
        }
    }

    @Test
    fun `on ShareClick action with zipUriToCache Success should send ShareLog event`() =
        runTest {
            val viewModel = createViewModel()
            val data = mockk<FlightRecorderDataSet.FlightRecorderData> {
                every { id } returns "50"
                every { fileName } returns "filename"
            }
            val dataset = FlightRecorderDataSet(data = setOf(data))
            every { settingsRepository.flightRecorderData } returns dataset
            val item = mockk<RecordedLogsState.DisplayItem> {
                every { id } returns "50"
            }
            val zipFile = mockk<File>()
            coEvery {
                fileManager.zipUriToCache(uri = any())
            } returns ZipFileResult.Success(zipFile)

            viewModel.eventFlow.test {
                viewModel.trySendAction(RecordedLogsAction.ShareClick(item))
                assertEquals(RecordedLogsEvent.ShareLog(uri = zipFile.toString()), awaitItem())
            }

            coVerify(exactly = 1) {
                fileManager.zipUriToCache(uri = any())
            }
        }

    @Test
    fun `on ShareClick action with zipUriToCache Failure should display an error dialog`() {
        val viewModel = createViewModel()
        val data = mockk<FlightRecorderDataSet.FlightRecorderData> {
            every { id } returns "50"
            every { fileName } returns "filename"
        }
        val dataset = FlightRecorderDataSet(data = setOf(data))
        every { settingsRepository.flightRecorderData } returns dataset
        val item = mockk<RecordedLogsState.DisplayItem> {
            every { id } returns "50"
        }
        val error = Throwable("Fail!")
        coEvery { fileManager.zipUriToCache(uri = any()) } returns ZipFileResult.Failure(error)

        viewModel.trySendAction(RecordedLogsAction.ShareClick(item))

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = RecordedLogsState.DialogState.Error(
                    title = BitwardenString.unable_to_share.asText(),
                    message = BitwardenString.please_try_again_or_select_a_different_log.asText(),
                    error = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            fileManager.zipUriToCache(uri = any())
        }
    }

    @Test
    fun `on ShareClick action with zipUriToCache NothingToZip should display an error dialog`() {
        val viewModel = createViewModel()
        val data = mockk<FlightRecorderDataSet.FlightRecorderData> {
            every { id } returns "50"
            every { fileName } returns "filename"
        }
        val dataset = FlightRecorderDataSet(data = setOf(data))
        every { settingsRepository.flightRecorderData } returns dataset
        val item = mockk<RecordedLogsState.DisplayItem> {
            every { id } returns "50"
        }
        coEvery { fileManager.zipUriToCache(uri = any()) } returns ZipFileResult.NothingToZip

        viewModel.trySendAction(RecordedLogsAction.ShareClick(item))

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = RecordedLogsState.DialogState.Error(
                    title = BitwardenString.unable_to_share.asText(),
                    message = BitwardenString.the_log_file_you_are_trying_to_share_has_been_removed
                        .asText(),
                    error = null,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            fileManager.zipUriToCache(uri = any())
        }
    }

    @Test
    fun `on DeleteAllClick action should call deleteAllLogs`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(RecordedLogsAction.DeleteAllClick)
            assertEquals(
                RecordedLogsEvent.ShowSnackbar(BitwardenString.all_logs_deleted.asText()),
                awaitItem(),
            )
        }
        verify(exactly = 1) {
            settingsRepository.deleteAllLogs()
        }
    }

    @Test
    fun `on DeleteClick action should call deleteLog`() = runTest {
        val data = mockk<FlightRecorderDataSet.FlightRecorderData> {
            every { id } returns "50"
        }
        val dataset = FlightRecorderDataSet(data = setOf(data))
        every { settingsRepository.flightRecorderData } returns dataset
        val viewModel = createViewModel()
        val item = mockk<RecordedLogsState.DisplayItem> {
            every { id } returns "50"
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(RecordedLogsAction.DeleteClick(item = item))
            assertEquals(
                RecordedLogsEvent.ShowSnackbar(BitwardenString.log_deleted.asText()),
                awaitItem(),
            )
        }
        verify(exactly = 1) {
            settingsRepository.deleteLog(data = data)
        }
    }

    @Test
    fun `on DismissDialog action should clear the dialog state`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = RecordedLogsState.DialogState.Error(
                title = "title".asText(),
                message = "message".asText(),
                error = null,
            ),
        )
        val viewModel = createViewModel(state = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(RecordedLogsAction.DismissDialog)
            assertEquals(initialState.copy(dialogState = null), awaitItem())
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

    private fun setupMockUri(
        url: String,
    ): Uri {
        val mockUri = mockk<Uri>()
        every { Uri.parse(url) } returns mockUri
        return mockUri
    }
}

private val DEFAULT_STATE: RecordedLogsState =
    RecordedLogsState(
        viewState = RecordedLogsState.ViewState.Empty,
        dialogState = null,
        logsFolder = "/logs",
    )

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
