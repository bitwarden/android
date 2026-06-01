package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.data.util.toFormattedDateStyle
import com.bitwarden.core.data.util.toFormattedTimeStyle
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import java.time.Clock
import java.time.Instant
import java.time.format.FormatStyle

/**
 * Helper function to create a [BitwardenSnackbarData] representing the active flight recorder.
 */
fun FlightRecorderDataSet.toSnackbarData(
    clock: Clock,
): BitwardenSnackbarData? {
    val expirationTime = this
        .data
        .find { it.isActive && !it.isBannerDismissed }
        ?.let { Instant.ofEpochMilli(it.startTimeMs + it.durationMs) }
        ?: return null
    return BitwardenSnackbarData(
        message = BitwardenString.flight_recorder_banner_message.asText(
            expirationTime.toFormattedDateStyle(dateStyle = FormatStyle.SHORT, clock = clock),
            expirationTime.toFormattedTimeStyle(timeStyle = FormatStyle.SHORT, clock = clock),
        ),
        messageHeader = BitwardenString.flight_recorder_banner_title.asText(),
        actionLabel = BitwardenString.go_to_settings.asText(),
        withDismissAction = true,
    )
}
