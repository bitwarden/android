package com.bitwarden.authenticator.ui.platform.util

import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * Converts the [TemporalAccessor] to a formatted string based on the provided pattern and timezone.
 */
fun TemporalAccessor.toFormattedPattern(
    pattern: String,
    zone: ZoneId,
): String = DateTimeFormatter.ofPattern(pattern).withZone(zone).format(this)

/**
 * Converts the [TemporalAccessor] to a formatted string based on the provided pattern and timezone.
 */
fun TemporalAccessor.toFormattedPattern(
    pattern: String,
    clock: Clock = Clock.systemDefaultZone(),
): String = toFormattedPattern(pattern = pattern, zone = clock.zone)
