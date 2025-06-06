package com.x8bit.bitwarden.ui.platform.feature.settings.about.util

import com.bitwarden.core.data.util.toFormattedPattern
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import java.time.Clock
import java.time.Instant

/**
 * Creates a properly formatted string indicating when the logging will stop for the active log or
 * null if there is no active log.
 */
fun FlightRecorderDataSet.getStopsLoggingStringForActiveLog(
    clock: Clock = Clock.systemDefaultZone(),
): Text? = this.activeFlightRecorderData?.getStopsLoggingString(clock = clock)

/**
 * Creates a properly formatted string indicating when the logging will stop.
 */
private fun FlightRecorderDataSet.FlightRecorderData.getStopsLoggingString(
    clock: Clock,
): Text {
    val completionInstant = Instant.ofEpochMilli(this.startTimeMs + this.durationMs)
    val completionDate = completionInstant.toFormattedPattern(pattern = "M/d/yy", clock = clock)
    val completionTime = completionInstant.toFormattedPattern(pattern = "h:mm a", clock = clock)
    return R.string.stops_logging_on.asText(completionDate, completionTime)
}
