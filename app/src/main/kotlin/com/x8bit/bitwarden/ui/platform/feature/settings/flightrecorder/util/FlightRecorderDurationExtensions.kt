package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util

import com.bitwarden.data.manager.model.FlightRecorderDuration
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * A helper function to map the [FlightRecorderDuration] to a displayable label.
 */
val FlightRecorderDuration.displayText: Text
    get() = when (this) {
        FlightRecorderDuration.ONE_HOUR -> BitwardenString.flight_recorder_one_hour.asText()
        FlightRecorderDuration.EIGHT_HOURS -> BitwardenString.flight_recorder_eight_hours.asText()
        FlightRecorderDuration.TWENTY_FOUR_HOURS -> {
            BitwardenString.flight_recorder_twenty_four_hours.asText()
        }

        FlightRecorderDuration.ONE_WEEK -> BitwardenString.flight_recorder_one_week.asText()
    }
