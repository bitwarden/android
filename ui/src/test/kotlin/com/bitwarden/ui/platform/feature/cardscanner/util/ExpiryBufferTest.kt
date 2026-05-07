package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExpiryBufferTest {

    @Test
    fun `record returns null when no expiry has ever been seen`() {
        val buffer = ExpiryBuffer()
        assertNull(buffer.record(month = null, year = null))
    }

    @Test
    fun `record returns the recorded expiry on the same frame it is observed`() {
        val buffer = ExpiryBuffer()
        assertEquals(
            ExpiryBuffer.Expiry(month = "12", year = "2028"),
            buffer.record(month = "12", year = "2028"),
        )
    }

    @Test
    fun `record returns the latest expiry seen even when later frames produce no expiry`() {
        val buffer = ExpiryBuffer()

        buffer.record(month = "12", year = "2028")
        assertEquals(
            ExpiryBuffer.Expiry(month = "12", year = "2028"),
            buffer.record(month = null, year = null),
        )
    }

    @Test
    fun `record returns null once the only observed expiry has aged out of the window`() {
        val buffer = ExpiryBuffer()

        buffer.record(month = "12", year = "2028")
        buffer.record(month = null, year = null)
        buffer.record(month = null, year = null)
        // Window size is 3; the original observation is now displaced by this fourth frame.
        assertNull(buffer.record(month = null, year = null))
    }

    @Test
    fun `record overwrites an older expiry with the most recent observation`() {
        val buffer = ExpiryBuffer()

        buffer.record(month = "01", year = "2027")
        assertEquals(
            ExpiryBuffer.Expiry(month = "12", year = "2028"),
            buffer.record(month = "12", year = "2028"),
        )
    }

    @Test
    fun `record preserves a month-only expiry when no year was parsed`() {
        val buffer = ExpiryBuffer()
        assertEquals(
            ExpiryBuffer.Expiry(month = "12", year = null),
            buffer.record(month = "12", year = null),
        )
    }
}
