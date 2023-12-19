package com.x8bit.bitwarden.ui.tools.feature.generator.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Converts the [Instant] to a formatted string based on the provided pattern and time zone.
 */
fun Instant.toFormattedPattern(
    pattern: String,
    zone: ZoneId = TimeZone.getDefault().toZoneId(),
): String {
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone)
    return formatter.format(this)
}
