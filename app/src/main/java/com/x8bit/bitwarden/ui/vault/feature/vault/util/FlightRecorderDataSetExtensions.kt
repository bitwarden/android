package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import java.time.Clock
import java.time.Instant

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
        message = R.string.flight_recorder_banner_message.asText(
            expirationTime.toFormattedPattern(pattern = "M/d/yy", clock = clock),
            expirationTime.toFormattedPattern(pattern = "h:mm a", clock = clock),
        ),
        messageHeader = R.string.flight_recorder_banner_title.asText(),
        actionLabel = R.string.go_to_settings.asText(),
        withDismissAction = true,
    )
}
