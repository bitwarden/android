package com.x8bit.bitwarden.data.platform.base.util

import com.x8bit.bitwarden.ui.platform.base.util.isOverFiveMinutesOld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.TimeZone

class ZonedDateTimeExtensionsTest {
    @BeforeEach
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterEach
    fun teardown() {
        TimeZone.setDefault(null)
    }

    @Test
    fun `isOverFiveMinutesOld returns true when time is old`() {
        val time = ZonedDateTime.parse("2022-09-13T00:00Z")
        assertTrue(time.isOverFiveMinutesOld())
    }

    @Test
    fun `isOverFiveMinutesOld returns false when time is now`() {
        val time = ZonedDateTime.now()
        assertFalse(time.isOverFiveMinutesOld())
    }

    @Test
    fun `isOverFiveMinutesOld returns false when time is now minus 5 minutes`() {
        val time = ZonedDateTime.now().minusMinutes(5)
        assertFalse(time.isOverFiveMinutesOld())
    }

    @Test
    fun `isOverFiveMinutesOld returns true when time is now minus 6 minutes`() {
        val time = ZonedDateTime.now().minusMinutes(6)
        assertTrue(time.isOverFiveMinutesOld())
    }
}
