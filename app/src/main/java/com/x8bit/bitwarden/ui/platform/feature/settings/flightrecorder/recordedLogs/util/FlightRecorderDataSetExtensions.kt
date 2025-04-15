package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.data.platform.util.fileOf
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.RecordedLogsState
import com.x8bit.bitwarden.ui.platform.util.formatBytes
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import kotlinx.collections.immutable.toImmutableList
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Converts a set of [FlightRecorderDataSet] to a [RecordedLogsState.ViewState].
 */
fun FlightRecorderDataSet.toViewState(
    clock: Clock,
    logsFolder: String,
): RecordedLogsState.ViewState =
    if (this.data.isEmpty()) {
        RecordedLogsState.ViewState.Empty
    } else {
        RecordedLogsState.ViewState.Content(
            items = this
                .data
                .sortedByDescending { it.startTimeMs }
                .map { it.toDisplayItem(clock = clock, logsFolder = logsFolder) }
                .toImmutableList(),
        )
    }

private fun FlightRecorderDataSet.FlightRecorderData.toDisplayItem(
    clock: Clock,
    logsFolder: String,
): RecordedLogsState.DisplayItem =
    RecordedLogsState.DisplayItem(
        id = this.id,
        title = this.title(clock = clock).asText(),
        subtextStart = this.getFileSize(logsFolder = logsFolder).asText(),
        subtextEnd = this.expiresIn(clock = clock),
        isDeletedEnabled = !this.isActive,
    )

private fun FlightRecorderDataSet.FlightRecorderData.title(clock: Clock): String {
    val pattern = "yyyy-MM-dd'T'HH:mm:ss"
    val formattedStartTime = Instant
        .ofEpochMilli(this.startTimeMs)
        .toFormattedPattern(pattern = pattern, clock = clock)
    val formattedEndTime = Instant
        .ofEpochMilli(this.startTimeMs + this.durationMs)
        .toFormattedPattern(pattern = pattern, clock = clock)
    return "$formattedStartTime – $formattedEndTime"
}

private fun FlightRecorderDataSet.FlightRecorderData.expiresIn(clock: Clock): Text? {
    val expirationTime = this.expirationTimeMs?.let { Instant.ofEpochMilli(it) } ?: return null
    val now = clock.instant()
    val dayBeforeExpiration = expirationTime.minus(1, ChronoUnit.DAYS).atZone(clock.zone)
    return if (this.isActive) {
        // If the log is active, then the expiration time should be null but we check here anyways.
        null
    } else if (now.isAfter(expirationTime)) {
        // We have passed expiration. This should never happen since the data should be deleted.
        R.string.expired.asText()
    } else if (now.isAfter(expirationTime.minus(1, ChronoUnit.DAYS))) {
        // We are within 24 hours of expiration, so show the specific time.
        val expirationTime = expirationTime.toFormattedPattern(pattern = "h:mm a", clock = clock)
        R.string.expires_at.asText(expirationTime)
    } else if (dayBeforeExpiration.dayOfYear == now.atZone(clock.zone).dayOfYear) {
        // We expire tomorrow based on the day of year.
        R.string.expires_tomorrow.asText()
    } else {
        // Let them know how many days they have left.
        val millisRemaining = expirationTime.minusMillis(now.toEpochMilli()).toEpochMilli()
        R.string.expires_in_days.asText(millisRemaining.milliseconds.inWholeDays)
    }
}

private fun FlightRecorderDataSet.FlightRecorderData.getFileSize(
    logsFolder: String,
): String = fileOf("$logsFolder/${this.fileName}")
    .length()
    .formatBytes()
