package com.bitwarden.core.data.util

import java.time.Clock
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

/**
 * Converts the [TemporalAccessor] to a formatted string based on the provided pattern and timezone.
 */
fun TemporalAccessor.toFormattedPattern(
    pattern: String,
    clock: Clock = Clock.systemDefaultZone(),
): String = DateTimeFormatter.ofPattern(pattern).withZone(clock.zone).format(this)

/**
 * Converts the [TemporalAccessor] to a formatted date string based on the provided style, locale,
 * and clock.
 *
 * In US English, the output string will be as follows for the given [dateStyle]:
 * * [FormatStyle.SHORT]: 6/6/25
 * * [FormatStyle.MEDIUM]: Jun 6, 2025
 * * [FormatStyle.LONG]: June 6, 2025
 * * [FormatStyle.FULL]: Friday, June 6, 2025
 */
fun TemporalAccessor.toFormattedDateStyle(
    dateStyle: FormatStyle,
    locale: Locale = Locale.getDefault(),
    clock: Clock = Clock.systemDefaultZone(),
): String =
    toFormattedPattern(
        pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            dateStyle,
            null,
            IsoChronology.INSTANCE,
            locale,
        ),
        clock = clock,
    )

/**
 * Converts the [TemporalAccessor] to a formatted time string based on the provided style, locale,
 * and clock.
 *
 * In US English, the output string will be as follows for the given [timeStyle]:
 * * [FormatStyle.SHORT]: 4:15 PM
 * * [FormatStyle.MEDIUM]: 4:15:21 PM
 * * [FormatStyle.LONG]: 4:15:21 PM CDT
 * * [FormatStyle.FULL]: 4:51:03 PM Central Daylight Time
 */
fun TemporalAccessor.toFormattedTimeStyle(
    timeStyle: FormatStyle,
    locale: Locale = Locale.getDefault(),
    clock: Clock = Clock.systemDefaultZone(),
): String =
    toFormattedPattern(
        pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            null,
            timeStyle,
            IsoChronology.INSTANCE,
            locale,
        ),
        clock = clock,
    )

/**
 * Converts the [TemporalAccessor] to a formatted string based on the provided style, locale, and
 * clock.
 */
fun TemporalAccessor.toFormattedDateTimeStyle(
    dateStyle: FormatStyle,
    timeStyle: FormatStyle,
    locale: Locale = Locale.getDefault(),
    clock: Clock = Clock.systemDefaultZone(),
): String =
    toFormattedPattern(
        pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            dateStyle,
            timeStyle,
            IsoChronology.INSTANCE,
            locale,
        ),
        clock = clock,
    )
