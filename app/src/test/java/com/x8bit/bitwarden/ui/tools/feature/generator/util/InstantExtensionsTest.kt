package com.x8bit.bitwarden.ui.tools.feature.generator.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class InstantExtensionsTest {

    @Test
    fun `toFormattedPattern should return correctly formatted string`() {
        val instant = Instant.parse("2023-12-10T15:30:00Z")
        val pattern = "MM/dd/yyyy hh:mm a"
        val zone = ZoneId.of("UTC")
        val expectedFormattedString = "12/10/2023 03:30 PM"
        val formattedString = instant.toFormattedPattern(pattern, zone)

        assertEquals(expectedFormattedString, formattedString)
    }
}
