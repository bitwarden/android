package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.resource.BitwardenString
import java.time.Instant
import java.time.format.FormatStyle

private const val MINUTES_PER_HOUR = 60L

/**
 * Formats a length of time given in [minutes] as, for example, "45 min", "2 h" or "1 h 30 min".
 */
@Composable
fun pamDurationText(minutes: Long): String {
    val hours = minutes / MINUTES_PER_HOUR
    val remainder = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0L -> stringResource(BitwardenString.pam_duration_minutes_value, remainder)
        remainder == 0L -> stringResource(BitwardenString.pam_duration_hours_value, hours)
        else -> stringResource(
            BitwardenString.pam_duration_hours_minutes_value,
            hours,
            remainder,
        )
    }
}

/**
 * Formats the access window as "<start> to <end>" in the user's locale and time zone.
 */
@Composable
fun pamWindowText(start: Instant, end: Instant): String {
    val clock = LocalClock.current
    return stringResource(
        BitwardenString.pam_window_range,
        start.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.SHORT,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
        end.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.SHORT,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
    )
}
