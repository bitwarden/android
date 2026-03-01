package com.bitwarden.core.util

import java.time.Instant

private const val NANOS_PER_TICK = 100L
private const val TICKS_PER_SECOND = 1000000000L / NANOS_PER_TICK

/**
 * Seconds offset from 01/01/1970 to 01/01/0001.
 */
private const val YEAR_OFFSET = -62135596800L

/**
 * Returns the [Instant] of the binary [Long] [value]. This is needed to remain consistent
 * with how `DateTime`s were stored when using C#.
 *
 * This functionality is based on the https://stackoverflow.com/questions/65315060/how-to-convert-net-datetime-tobinary-to-java-date
 */
@Suppress("MagicNumber")
fun getInstantFromBinaryLong(value: Long): Instant {
    // Shift the bits to eliminate the "Kind" property since we know it was stored as UTC and leave
    // us with ticks
    val ticks = value and (1L shl 62) - 1
    return Instant.ofEpochSecond(
        ticks / TICKS_PER_SECOND + YEAR_OFFSET,
        ticks % TICKS_PER_SECOND * NANOS_PER_TICK,
    )
}

/**
 * Returns the [Instant] [value] converted to a binary [Long]. This is needed to remain
 * consistent with how `DateTime`s were stored when using C#.
 *
 * This functionality is based on the https://stackoverflow.com/questions/65315060/how-to-convert-net-datetime-tobinary-to-java-date
 */
@Suppress("MagicNumber")
fun getBinaryLongFromInstant(value: Instant): Long {
    val nanoAdjustment = value.nano / NANOS_PER_TICK
    val ticks = (value.epochSecond - YEAR_OFFSET) * TICKS_PER_SECOND + nanoAdjustment
    return 1L shl 62 or ticks
}
