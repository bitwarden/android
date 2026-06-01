package com.x8bit.bitwarden.ui.platform.feature.settings.about.util

import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FlightRecorderDataSetExtensionsTest {
    @Test
    fun `getStopsLoggingStringForActiveLog returns null when there is no active logger`() {
        val dataset1 = FlightRecorderDataSet(data = setOf())
        val result1 = dataset1.getStopsLoggingStringForActiveLog(clock = FIXED_CLOCK)
        assertNull(result1)

        val dataset2 = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "51",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_059_882L,
                    durationMs = 3_600L,
                    isActive = false,
                ),
            ),
        )
        val result2 = dataset2.getStopsLoggingStringForActiveLog(clock = FIXED_CLOCK)
        assertNull(result2)
    }

    @Test
    fun `getStopsLoggingStringForActiveLog returns correct text when there is an active logger`() {
        val dataset = FlightRecorderDataSet(
            data = setOf(
                FlightRecorderDataSet.FlightRecorderData(
                    id = "51",
                    fileName = "flight_recorder_2025-04-03_14-22-40",
                    startTimeMs = 1_744_059_882L,
                    durationMs = 3_600L,
                    isActive = true,
                ),
            ),
        )
        val result = dataset.getStopsLoggingStringForActiveLog(clock = FIXED_CLOCK)
        assertEquals(BitwardenString.stops_logging_on.asText("1/21/70", "4:27\u202FAM"), result)
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
