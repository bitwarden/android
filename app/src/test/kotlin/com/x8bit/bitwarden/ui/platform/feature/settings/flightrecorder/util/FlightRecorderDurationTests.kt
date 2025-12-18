package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util

import com.bitwarden.data.manager.model.FlightRecorderDuration
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlightRecorderDurationTests {
    @Test
    fun `displayText should return appropriate text`() {
        mapOf(
            FlightRecorderDuration.ONE_HOUR to BitwardenString.flight_recorder_one_hour.asText(),
            FlightRecorderDuration.EIGHT_HOURS to
                BitwardenString.flight_recorder_eight_hours.asText(),
            FlightRecorderDuration.TWENTY_FOUR_HOURS to
                BitwardenString.flight_recorder_twenty_four_hours.asText(),
            FlightRecorderDuration.ONE_WEEK to BitwardenString.flight_recorder_one_week.asText(),
        )
            .forEach {
                assertEquals(it.value, it.key.displayText)
            }
    }
}
