package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FlightRecorderDataSetExtensionsTest {
    @Test
    fun `toSnackbarData with empty data should return null`() {
        val dataset = FlightRecorderDataSet(data = emptySet())

        val result = dataset.toSnackbarData(clock = FIXED_CLOCK)

        assertNull(result)
    }

    @Test
    fun `toSnackbarData with no active logs should return null`() {
        val dataset = FlightRecorderDataSet(
            data = setOf(
                DEFAULT_DATA.copy(
                    isActive = false,
                    isBannerDismissed = false,
                ),
            ),
        )

        val result = dataset.toSnackbarData(clock = FIXED_CLOCK)

        assertNull(result)
    }

    @Test
    fun `toSnackbarData with active logs but dismissed banner should return null`() {
        val dataset = FlightRecorderDataSet(
            data = setOf(
                DEFAULT_DATA.copy(
                    isActive = true,
                    isBannerDismissed = true,
                ),
            ),
        )

        val result = dataset.toSnackbarData(clock = FIXED_CLOCK)

        assertNull(result)
    }

    @Test
    fun `toSnackbarData with active logs and un-dismissed banner should return SnackbarData`() {
        val dataset = FlightRecorderDataSet(
            data = setOf(
                DEFAULT_DATA.copy(
                    isActive = true,
                    isBannerDismissed = false,
                ),
            ),
        )

        val result = dataset.toSnackbarData(clock = FIXED_CLOCK)

        assertEquals(
            BitwardenSnackbarData(
                message = BitwardenString.flight_recorder_banner_message
                    .asText("4/12/25", "9:15\u202FAM"),
                messageHeader = BitwardenString.flight_recorder_banner_title.asText(),
                actionLabel = BitwardenString.go_to_settings.asText(),
                withDismissAction = true,
            ),
            result,
        )
    }
}

private val DEFAULT_DATA = FlightRecorderDataSet.FlightRecorderData(
    id = "50",
    fileName = "flight_recorder",
    startTimeMs = 1_744_445_752_855L,
    durationMs = 3_600_000L,
    expirationTimeMs = null,
    isActive = false,
    isBannerDismissed = false,
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
