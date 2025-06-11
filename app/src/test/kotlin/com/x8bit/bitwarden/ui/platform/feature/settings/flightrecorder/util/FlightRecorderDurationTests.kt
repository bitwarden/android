package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util

import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlightRecorderDurationTests {
    @Test
    fun `displayText should return appropriate text`() {
        mapOf(
            FlightRecorderDuration.ONE_HOUR to R.string.flight_recorder_one_hour.asText(),
            FlightRecorderDuration.EIGHT_HOURS to R.string.flight_recorder_eight_hours.asText(),
            FlightRecorderDuration.TWENTY_FOUR_HOURS to
                R.string.flight_recorder_twenty_four_hours.asText(),
            FlightRecorderDuration.ONE_WEEK to R.string.flight_recorder_one_week.asText(),
        )
            .forEach {
                assertEquals(it.value, it.key.displayText)
            }
    }
}
