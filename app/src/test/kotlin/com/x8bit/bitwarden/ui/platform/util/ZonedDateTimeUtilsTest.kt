package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.data.platform.util.getBinaryLongFromZoneDateTime
import com.x8bit.bitwarden.data.platform.util.getZoneDateTimeFromBinaryLong
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ZonedDateTimeUtilsTest {
    @Test
    fun `getZoneDateTimeFromBinaryLong should correctly convert a Long to a ZonedDateTime`() {
        val binaryLong = 5250087787086431044L
        val expectedDateTime = ZonedDateTime.parse("2024-01-06T22:27:45.904314Z")
        assertEquals(expectedDateTime, getZoneDateTimeFromBinaryLong(binaryLong))

        val a = getZoneDateTimeFromBinaryLong(binaryLong)
        val b = getBinaryLongFromZoneDateTime(a)
        assertEquals(binaryLong, b)
    }

    @Test
    fun `getBinaryLongFromZoneDateTime should correctly convert a ZonedDateTime to a Long`() {
        val dateTime = ZonedDateTime.parse("2024-01-06T22:27:45.904314Z")
        val expectedBinaryLong = 5250087787086431044L
        assertEquals(expectedBinaryLong, getBinaryLongFromZoneDateTime(dateTime))
    }
}
