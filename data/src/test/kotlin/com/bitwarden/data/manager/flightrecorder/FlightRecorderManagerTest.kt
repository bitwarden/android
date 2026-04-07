package com.bitwarden.data.manager.flightrecorder

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.datasource.disk.util.FakeFlightRecorderDiskSource
import com.bitwarden.data.manager.model.FlightRecorderDuration
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class FlightRecorderManagerTest {
    private val broadcastReceiver = slot<BroadcastReceiver>()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private val context: Context = mockk {
        every { registerReceiver(capture(broadcastReceiver), any()) } returns null
    }
    private val fakeFlightRecorderDiskSource = FakeFlightRecorderDiskSource()
    private val flightRecorderWriter = mockk<FlightRecorderWriter> {
        coEvery { getOrCreateLogFile(data = any()) } returns mockk<File>().asSuccess()
        coEvery { deleteLogs(dataset = any()) } just runs
        coEvery { deleteLog(data = any()) } just runs
        coEvery {
            writeToLog(
                data = any(),
                priority = any(),
                tag = any(),
                message = any(),
                throwable = any(),
            )
        } just runs
    }
    private val fakeDispatcherManager = FakeDispatcherManager()

    private val flightRecorder = FlightRecorderManagerImpl(
        context = context,
        clock = FIXED_CLOCK,
        flightRecorderDiskSource = fakeFlightRecorderDiskSource,
        flightRecorderWriter = flightRecorderWriter,
        dispatcherManager = fakeDispatcherManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "mockUUID"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `flightRecorderData should pull from and update SettingsDiskSource`() {
        fakeFlightRecorderDiskSource.flightRecorderData = null
        assertEquals(FlightRecorderDataSet(data = emptySet()), flightRecorder.flightRecorderData)

        val expected = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "mockUUID",
                    fileName = "flight_recorder_2023-10-27_12-00-00",
                    startTimeMs = FIXED_CLOCK_TIME,
                    durationMs = FlightRecorderDuration.ONE_HOUR.milliseconds,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(1L, ChronoUnit.HOURS)
                        .toEpochMilli(),
                ),
            ),
        )
        fakeFlightRecorderDiskSource.flightRecorderData = expected
        assertEquals(expected, flightRecorder.flightRecorderData)
    }

    @Test
    fun `flightRecorderDataFlow should react to changes in SettingsDiskSource`() = runTest {
        fakeFlightRecorderDiskSource.flightRecorderData = null
        flightRecorder
            .flightRecorderDataFlow
            .test {
                assertEquals(FlightRecorderDataSet(data = emptySet()), awaitItem())

                val unexpired = FlightRecorderDataSet(
                    data = setOf(
                        FlightRecorderDataSet.FlightRecorderData(
                            id = "mockUUID",
                            fileName = "flight_recorder_2023-10-27_12-00-00",
                            startTimeMs = FIXED_CLOCK_TIME,
                            durationMs = FlightRecorderDuration.ONE_HOUR.milliseconds,
                            isActive = false,
                            expirationTimeMs = FIXED_CLOCK
                                .instant()
                                .plus(1L, ChronoUnit.HOURS)
                                .toEpochMilli(),
                        ),
                    ),
                )
                fakeFlightRecorderDiskSource.flightRecorderData = unexpired
                assertEquals(unexpired, awaitItem())

                val expired = FlightRecorderDataSet(
                    data = setOf(
                        FlightRecorderDataSet.FlightRecorderData(
                            id = "mockUUID",
                            fileName = "flight_recorder_2023-10-27_12-00-00",
                            startTimeMs = FIXED_CLOCK_TIME,
                            durationMs = FlightRecorderDuration.ONE_HOUR.milliseconds,
                            isActive = false,
                            expirationTimeMs = FIXED_CLOCK
                                .instant()
                                .minus(1L, ChronoUnit.HOURS)
                                .toEpochMilli(),
                        ),
                    ),
                )
                fakeFlightRecorderDiskSource.flightRecorderData = expired
                assertEquals(FlightRecorderDataSet(data = emptySet()), awaitItem())

                fakeFlightRecorderDiskSource.flightRecorderData = null
                expectNoEvents()
            }
    }

    @Test
    fun `startFlightRecorder should properly update SettingsDiskSource`() {
        fakeFlightRecorderDiskSource.flightRecorderData = null

        flightRecorder.startFlightRecorder(duration = FlightRecorderDuration.ONE_HOUR)

        fakeFlightRecorderDiskSource.assertFlightRecorderData(
            expected = FlightRecorderDataSet(
                data = setOf(
                    FlightRecorderDataSet.FlightRecorderData(
                        id = "mockUUID",
                        fileName = "flight_recorder_2023-10-27_12-00-00.txt",
                        startTimeMs = FIXED_CLOCK_TIME,
                        durationMs = FlightRecorderDuration.ONE_HOUR.milliseconds,
                        isActive = true,
                    ),
                ),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `dismissFlightRecorderBanner should set the isDismissBanner flag to true and update SettingsDiskSource`() {
        val data = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "40",
                    fileName = "fileName1",
                    startTimeMs = FIXED_CLOCK_TIME,
                    durationMs = 60L,
                    isActive = true,
                    expirationTimeMs = null,
                    isBannerDismissed = false,
                ),
                FlightRecorderDataSet.FlightRecorderData(
                    id = "50",
                    fileName = "fileName2",
                    startTimeMs = FIXED_CLOCK_TIME,
                    durationMs = 60L,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(30, ChronoUnit.DAYS)
                        .toEpochMilli(),
                    isBannerDismissed = false,
                ),
            ),
        )
        fakeFlightRecorderDiskSource.flightRecorderData = data

        flightRecorder.dismissFlightRecorderBanner()

        fakeFlightRecorderDiskSource.assertFlightRecorderData(
            expected = FlightRecorderDataSet(
                data = setOf(
                    FlightRecorderDataSet.FlightRecorderData(
                        id = "40",
                        fileName = "fileName1",
                        startTimeMs = FIXED_CLOCK_TIME,
                        durationMs = 60L,
                        isActive = true,
                        expirationTimeMs = null,
                        isBannerDismissed = true,
                    ),
                    FlightRecorderDataSet.FlightRecorderData(
                        id = "50",
                        fileName = "fileName2",
                        startTimeMs = FIXED_CLOCK_TIME,
                        durationMs = 60L,
                        isActive = false,
                        expirationTimeMs = FIXED_CLOCK
                            .instant()
                            .plus(30, ChronoUnit.DAYS)
                            .toEpochMilli(),
                        isBannerDismissed = true,
                    ),
                ),
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `endFlightRecorder should set the active log to inactive and update the SettingsDiskSource`() {
        val data = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "40",
                    fileName = "fileName1",
                    startTimeMs = FIXED_CLOCK_TIME,
                    durationMs = 60L,
                    isActive = true,
                    expirationTimeMs = null,
                ),
                FlightRecorderDataSet.FlightRecorderData(
                    id = "50",
                    fileName = "fileName2",
                    startTimeMs = FIXED_CLOCK_TIME,
                    durationMs = 60L,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(30, ChronoUnit.DAYS)
                        .toEpochMilli(),
                ),
            ),
        )
        fakeFlightRecorderDiskSource.flightRecorderData = data

        flightRecorder.endFlightRecorder()

        fakeFlightRecorderDiskSource.assertFlightRecorderData(
            expected = FlightRecorderDataSet(
                data = setOf(
                    FlightRecorderDataSet.FlightRecorderData(
                        id = "40",
                        fileName = "fileName1",
                        startTimeMs = FIXED_CLOCK_TIME,
                        durationMs = 60L,
                        isActive = false,
                        expirationTimeMs = FIXED_CLOCK
                            .instant()
                            .plus(30, ChronoUnit.DAYS)
                            .toEpochMilli(),
                    ),
                    FlightRecorderDataSet.FlightRecorderData(
                        id = "50",
                        fileName = "fileName2",
                        startTimeMs = FIXED_CLOCK_TIME,
                        durationMs = 60L,
                        isActive = false,
                        expirationTimeMs = FIXED_CLOCK
                            .instant()
                            .plus(30, ChronoUnit.DAYS)
                            .toEpochMilli(),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `deleteAllLogs should clear the metadata for all non-active logs and call deleteLogs`() {
        val inactiveData = FlightRecorderDataSet.FlightRecorderData(
            id = "50",
            fileName = "fileName1",
            startTimeMs = FIXED_CLOCK_TIME,
            durationMs = 60L,
            isActive = false,
            expirationTimeMs = FIXED_CLOCK.instant().plus(1L, ChronoUnit.HOURS).toEpochMilli(),
        )
        val activeData = FlightRecorderDataSet.FlightRecorderData(
            id = "50",
            fileName = "fileName2",
            startTimeMs = FIXED_CLOCK_TIME,
            durationMs = 60L,
            isActive = true,
            expirationTimeMs = null,
        )
        val activeDataset = FlightRecorderDataSet(data = setOf(activeData))
        val inactiveDataset = FlightRecorderDataSet(data = setOf(inactiveData))

        fakeFlightRecorderDiskSource.flightRecorderData = FlightRecorderDataSet(
            data = setOf(activeData, inactiveData),
        )

        flightRecorder.deleteAllLogs()

        coVerify(exactly = 1) {
            flightRecorderWriter.deleteLogs(dataset = inactiveDataset)
        }
        fakeFlightRecorderDiskSource.assertFlightRecorderData(expected = activeDataset)
    }

    @Test
    fun `deleteLog with active log should do nothing`() {
        val data = FlightRecorderDataSet.FlightRecorderData(
            id = "50",
            fileName = "fileName1",
            startTimeMs = FIXED_CLOCK_TIME,
            durationMs = 60L,
            isActive = true,
        )
        val dataset = FlightRecorderDataSet(data = setOf(data))
        fakeFlightRecorderDiskSource.flightRecorderData = dataset

        flightRecorder.deleteLog(data = data)

        coVerify(exactly = 0) {
            flightRecorderWriter.deleteLog(data = data)
        }
        fakeFlightRecorderDiskSource.assertFlightRecorderData(expected = dataset)
    }

    @Test
    fun `deleteLog with inactive log should clear the metadata for the log and call deleteLog`() {
        val data = FlightRecorderDataSet.FlightRecorderData(
            id = "50",
            fileName = "fileName1",
            startTimeMs = FIXED_CLOCK_TIME,
            durationMs = 60L,
            isActive = false,
            expirationTimeMs = FIXED_CLOCK.instant().plus(1L, ChronoUnit.HOURS).toEpochMilli(),
        )
        fakeFlightRecorderDiskSource.flightRecorderData = FlightRecorderDataSet(data = setOf(data))

        flightRecorder.deleteLog(data = data)

        coVerify(exactly = 1) {
            flightRecorderWriter.deleteLog(data = data)
        }
        fakeFlightRecorderDiskSource.assertFlightRecorderData(
            expected = FlightRecorderDataSet(data = emptySet()),
        )
    }
}

private const val FIXED_CLOCK_TIME: Long = 1_698_408_000_000L
private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
