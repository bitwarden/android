package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DeviceLastActivityExtensionsTest {

    private val fixedClock = Clock.fixed(
        Instant.parse("2024-03-15T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `null activity date returns null`() {
        assertNull((null as Instant?).toLastActivityLabel(fixedClock))
    }

    @Test
    fun `same day returns today`() {
        val activityDate = Instant.parse("2024-03-15T00:00:00Z")
        assertEquals(BitwardenString.today.asText(), activityDate.toLastActivityLabel(fixedClock))
    }

    @Test
    fun `future date returns today`() {
        val activityDate = Instant.parse("2024-03-16T00:00:00Z")
        assertEquals(BitwardenString.today.asText(), activityDate.toLastActivityLabel(fixedClock))
    }

    @Test
    fun `1 day ago returns past seven days`() {
        val activityDate = Instant.parse("2024-03-14T00:00:00Z")
        assertEquals(
            BitwardenString.past_seven_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `6 days ago returns past seven days`() {
        val activityDate = Instant.parse("2024-03-09T00:00:00Z")
        assertEquals(
            BitwardenString.past_seven_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `7 days ago returns past fourteen days`() {
        val activityDate = Instant.parse("2024-03-08T00:00:00Z")
        assertEquals(
            BitwardenString.past_fourteen_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `13 days ago returns past fourteen days`() {
        val activityDate = Instant.parse("2024-03-02T00:00:00Z")
        assertEquals(
            BitwardenString.past_fourteen_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `14 days ago returns past thirty days`() {
        val activityDate = Instant.parse("2024-03-01T00:00:00Z")
        assertEquals(
            BitwardenString.past_thirty_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `29 days ago returns past thirty days`() {
        val activityDate = Instant.parse("2024-02-15T00:00:00Z")
        assertEquals(
            BitwardenString.past_thirty_days.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `30 days ago returns over thirty days ago`() {
        val activityDate = Instant.parse("2024-02-14T00:00:00Z")
        assertEquals(
            BitwardenString.over_thirty_days_ago.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }

    @Test
    fun `31 days ago returns over thirty days ago`() {
        val activityDate = Instant.parse("2024-02-13T00:00:00Z")
        assertEquals(
            BitwardenString.over_thirty_days_ago.asText(),
            activityDate.toLastActivityLabel(fixedClock),
        )
    }
}
