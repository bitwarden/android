package com.x8bit.bitwarden.ui.platform.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ZonedDateTimeExtensionsTest {

    @Test
    fun `orNow should return original ZonedDateTime when original is nonnull`() {
        val zonedDateTime = mockk<ZonedDateTime>()

        val result = zonedDateTime.orNow()

        assertEquals(zonedDateTime, result)
    }

    @Test
    fun `orNow should return current ZonedDateTime when original is null`() {
        val zonedDateTime = mockk<ZonedDateTime>()
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns zonedDateTime

        val result = null.orNow()

        assertEquals(zonedDateTime, result)
        unmockkStatic(ZonedDateTime::class)
    }
}
