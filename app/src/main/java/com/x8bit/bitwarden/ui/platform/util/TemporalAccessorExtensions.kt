package com.x8bit.bitwarden.ui.platform.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.TimeZone

/**
 * Converts the [TemporalAccessor] to a formatted string based on the provided pattern and timezone.
 */
fun TemporalAccessor.toFormattedPattern(
    pattern: String,
    zone: ZoneId = TimeZone.getDefault().toZoneId(),
): String {
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone)
    return formatter.format(this)
}
