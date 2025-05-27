package com.x8bit.bitwarden.ui.platform.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TemporalAccessorExtensionsTest {

    @Test
    fun `toFormattedPattern should return correctly formatted string with timezone`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")
        val pattern = "MM/dd/yyyy hh:mm a"
        val zone = ZoneId.of("UTC")
        val expectedFormattedString = "12/10/2023 03:30 PM"
        val formattedString = instant.toFormattedPattern(pattern, zone)

        assertEquals(expectedFormattedString, formattedString)
    }

    @Test
    fun `toFormattedPattern should return correctly formatted string with clock`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")
        val pattern = "MM/dd/yyyy hh:mm a"
        val clock: Clock = Clock.fixed(
            Instant.parse("2023-10-27T12:00:00Z"),
            ZoneOffset.UTC,
        )
        val expectedFormattedString = "12/10/2023 03:30 PM"
        val formattedString = instant.toFormattedPattern(pattern, clock)

        assertEquals(expectedFormattedString, formattedString)
    }
}
