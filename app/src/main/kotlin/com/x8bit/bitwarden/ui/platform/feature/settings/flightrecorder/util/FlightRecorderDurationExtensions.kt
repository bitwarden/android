package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration

/**
 * A helper function to map the [FlightRecorderDuration] to a displayable label.
 */
val FlightRecorderDuration.displayText: Text
    get() = when (this) {
        FlightRecorderDuration.ONE_HOUR -> R.string.flight_recorder_one_hour.asText()
        FlightRecorderDuration.EIGHT_HOURS -> R.string.flight_recorder_eight_hours.asText()
        FlightRecorderDuration.TWENTY_FOUR_HOURS -> {
            R.string.flight_recorder_twenty_four_hours.asText()
        }

        FlightRecorderDuration.ONE_WEEK -> R.string.flight_recorder_one_week.asText()
    }
