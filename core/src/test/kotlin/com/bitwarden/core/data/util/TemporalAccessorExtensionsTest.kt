package com.bitwarden.core.data.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.FormatStyle
import java.util.Locale

class TemporalAccessorExtensionsTest {

    @Test
    fun `toFormattedPattern should return correctly formatted string`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")

        assertEquals(
            "12/10/2023 03:30 PM",
            instant.toFormattedPattern(
                pattern = "MM/dd/yyyy hh:mm a",
                clock = FIXED_CLOCK,
            ),
        )
    }

    @Test
    fun `toFormattedDateStyle should return correctly formatted string with with locale`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")

        // US locale
        assertEquals(
            "12/10/23",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.SHORT,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Dec 10, 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.MEDIUM,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "December 10, 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.LONG,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Sunday, December 10, 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.FULL,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )

        // UK locale
        assertEquals(
            "10/12/2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.SHORT,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "10 Dec 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.MEDIUM,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "10 December 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.LONG,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Sunday, 10 December 2023",
            instant.toFormattedDateStyle(
                dateStyle = FormatStyle.FULL,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
    }

    @Test
    fun `toFormattedTimeStyle should return correctly formatted string with with locale`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")

        // US locale
        assertEquals(
            "3:30\u202FPM",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.SHORT,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "3:30:00\u202FPM",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.MEDIUM,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "3:30:00\u202FPM Z",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.LONG,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "3:30:00\u202FPM Z",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.FULL,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )

        // UK locale
        assertEquals(
            "15:30",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.SHORT,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "15:30:00",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.MEDIUM,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "15:30:00 Z",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.LONG,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "15:30:00 Z",
            instant.toFormattedTimeStyle(
                timeStyle = FormatStyle.FULL,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
    }

    @Test
    fun `toFormattedDateTimeStyle should return correctly formatted string with with locale`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")

        // US locale
        assertEquals(
            "12/10/23, 3:30\u202FPM",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.SHORT,
                timeStyle = FormatStyle.SHORT,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Dec 10, 2023, 3:30:00\u202FPM",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.MEDIUM,
                timeStyle = FormatStyle.MEDIUM,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "December 10, 2023, 3:30:00\u202FPM Z",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.LONG,
                timeStyle = FormatStyle.LONG,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Sunday, December 10, 2023, 3:30:00\u202FPM Z",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.FULL,
                timeStyle = FormatStyle.FULL,
                locale = Locale.US,
                clock = FIXED_CLOCK,
            ),
        )

        // UK locale
        assertEquals(
            "10/12/2023, 15:30",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.SHORT,
                timeStyle = FormatStyle.SHORT,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "10 Dec 2023, 15:30:00",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.MEDIUM,
                timeStyle = FormatStyle.MEDIUM,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "10 December 2023, 15:30:00 Z",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.LONG,
                timeStyle = FormatStyle.LONG,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
        assertEquals(
            "Sunday, 10 December 2023, 15:30:00 Z",
            instant.toFormattedDateTimeStyle(
                dateStyle = FormatStyle.FULL,
                timeStyle = FormatStyle.FULL,
                locale = Locale.UK,
                clock = FIXED_CLOCK,
            ),
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
