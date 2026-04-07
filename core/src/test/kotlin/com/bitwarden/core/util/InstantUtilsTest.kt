package com.bitwarden.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class InstantUtilsTest {
    @Test
    fun `getInstantFromBinaryLong should correctly convert a Long to an Instant`() {
        val binaryLong = 5250087787086431044L
        val expectedInstant = Instant.parse("2024-01-06T22:27:45.904314Z")
        assertEquals(expectedInstant, getInstantFromBinaryLong(binaryLong))

        val a = getInstantFromBinaryLong(binaryLong)
        val b = getBinaryLongFromInstant(a)
        assertEquals(binaryLong, b)
    }

    @Test
    fun `getBinaryLongFromInstant should correctly convert an Instant to a Long`() {
        val instant = Instant.parse("2024-01-06T22:27:45.904314Z")
        val expectedBinaryLong = 5250087787086431044L
        assertEquals(expectedBinaryLong, getBinaryLongFromInstant(instant))
    }
}
