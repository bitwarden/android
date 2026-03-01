package com.bitwarden.core.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InstantExtensionsTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `isOverFiveMinutesOld returns true when time is old`() {
        val time = Instant.parse("2022-09-13T00:00:00Z")
        assertTrue(time.isOverFiveMinutesOld(fixedClock))
    }

    @Test
    fun `isOverFiveMinutesOld returns false when time is now`() {
        val time = Instant.parse("2023-10-27T11:55:00Z")
        assertFalse(time.isOverFiveMinutesOld(fixedClock))
    }

    @Test
    fun `isOverFiveMinutesOld returns false when time is now minus 5 minutes`() {
        val time = fixedClock.instant().minusSeconds(300)
        assertFalse(time.isOverFiveMinutesOld(fixedClock))
    }

    @Test
    fun `isOverFiveMinutesOld returns true when time is now minus 6 minutes`() {
        val time = fixedClock.instant().minusSeconds(360)
        assertTrue(time.isOverFiveMinutesOld(fixedClock))
    }
}
