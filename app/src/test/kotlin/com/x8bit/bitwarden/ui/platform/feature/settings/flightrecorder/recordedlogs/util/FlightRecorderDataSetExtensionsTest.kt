package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedlogs.util

import com.bitwarden.core.util.fileOf
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.util.formatBytes
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util.toViewState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class FlightRecorderDataSetExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(
            ::fileOf,
            Long::formatBytes,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ::fileOf,
            Long::formatBytes,
        )
    }

    @Test
    fun `toViewState with empty data should return empty state`() {
        val dataset = FlightRecorderDataSet(data = emptySet())

        val result = dataset.toViewState(clock = FIXED_CLOCK, logsFolder = "/logs")

        assertEquals(RecordedLogsState.ViewState.Empty, result)
    }

    @Test
    fun `toViewState with data data should return content state`() {
        every { fileOf(path = any()).length() } returns 1024L
        every { 1024L.formatBytes() } returns "1.00 KB"
        val dataset = FlightRecorderDataSet(
            data = setOf(
                // Active
                FlightRecorderDataSet.FlightRecorderData(
                    id = "50",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_445_752_855L,
                    durationMs = 3_600_000L,
                    isActive = true,
                    expirationTimeMs = null,
                ),
                // Expires in 30 days
                FlightRecorderDataSet.FlightRecorderData(
                    id = "52",
                    fileName = "flight_recorder_2025-04-03_14-52-00",
                    startTimeMs = 1_744_445_700_000L,
                    durationMs = 3_600_000L,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(30, ChronoUnit.DAYS)
                        .toEpochMilli(),
                ),
                // Expires in less than 24 hours
                FlightRecorderDataSet.FlightRecorderData(
                    id = "51",
                    fileName = "flight_recorder_2025-04-03_14-52-00",
                    startTimeMs = 1_444_445_752_000L,
                    durationMs = 3_600_000L,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(12, ChronoUnit.HOURS)
                        .toEpochMilli(),
                ),
                // Expires tomorrow
                FlightRecorderDataSet.FlightRecorderData(
                    id = "53",
                    fileName = "flight_recorder_2025-04-03_14-52-00",
                    startTimeMs = 1_444_445_752_000L,
                    durationMs = 3_600_000L,
                    isActive = false,
                    expirationTimeMs = FIXED_CLOCK
                        .instant()
                        .plus(26, ChronoUnit.HOURS)
                        .toEpochMilli(),
                ),
            ),
        )

        val result = dataset.toViewState(clock = FIXED_CLOCK, logsFolder = "/logs")

        assertEquals(
            RecordedLogsState.ViewState.Content(
                items = persistentListOf(
                    RecordedLogsState.DisplayItem(
                        id = "50",
                        title = "2025-04-12T08:15:52 – 2025-04-12T09:15:52".asText(),
                        subtextStart = "1.00 KB".asText(),
                        subtextEnd = null,
                        isDeletedEnabled = false,
                    ),
                    RecordedLogsState.DisplayItem(
                        id = "52",
                        title = "2025-04-12T08:15:00 – 2025-04-12T09:15:00".asText(),
                        subtextStart = "1.00 KB".asText(),
                        subtextEnd = BitwardenString.expires_on.asText("5/11/25"),
                        isDeletedEnabled = true,
                    ),
                    RecordedLogsState.DisplayItem(
                        id = "51",
                        title = "2015-10-10T02:55:52 – 2015-10-10T03:55:52".asText(),
                        subtextStart = "1.00 KB".asText(),
                        subtextEnd = BitwardenString.expires_at.asText("10:15\u202FPM"),
                        isDeletedEnabled = true,
                    ),
                    RecordedLogsState.DisplayItem(
                        id = "53",
                        title = "2015-10-10T02:55:52 – 2015-10-10T03:55:52".asText(),
                        subtextStart = "1.00 KB".asText(),
                        subtextEnd = BitwardenString.expires_tomorrow.asText(),
                        isDeletedEnabled = true,
                    ),
                ),
            ),
            result,
        )
    }
}

private val FIXED_CLOCK = Clock.fixed(
    Instant.parse("2025-04-11T10:15:30.00Z"),
    ZoneOffset.UTC,
)
