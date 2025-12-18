package com.bitwarden.data.manager.model

/**
 * The selectable durations allowed for the flight recorder.
 */
enum class FlightRecorderDuration(
    val milliseconds: Long,
) {
    ONE_HOUR(milliseconds = 3_600_000L),
    EIGHT_HOURS(milliseconds = 28_800_000L),
    TWENTY_FOUR_HOURS(milliseconds = 86_400_000L),
    ONE_WEEK(milliseconds = 604_800_000L),
}
