package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemPamState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MINUTES_PER_HOUR = 60L
private const val ALERT_THRESHOLD_MINUTES = 5L

/**
 * The banner shown at the top of a gated vault item describing its privileged access state.
 *
 * @param pamState The privileged access state to display.
 * @param onRequestAccessClick Invoked when the user taps the request access button.
 * @param onStartAccessClick Invoked when the user taps start access on an approved request.
 * @param onCancelRequestClick Invoked when the user taps cancel request on an approved request.
 * @param modifier The [Modifier] to be applied to the banner.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemPamBanner(
    pamState: VaultItemPamState,
    onRequestAccessClick: () -> Unit,
    onStartAccessClick: () -> Unit,
    onCancelRequestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .standardHorizontalMargin()
            .padding(top = 12.dp),
    ) {
        when (pamState) {
            is VaultItemPamState.CanRequest -> {
                if (pamState.isSlotHeldByAnotherMember) {
                    BitwardenActionCard(
                        cardTitle = stringResource(id = BitwardenString.pam_slot_held_title),
                        cardSubtitle = pamState.slotFreesAt
                        ?.let { freesAt ->
                            stringResource(
                                id = BitwardenString.pam_slot_held_message_until,
                                DateTimeFormatter
                                    .ofLocalizedTime(FormatStyle.SHORT)
                                    .withZone(ZoneId.systemDefault())
                                    .format(freesAt),
                            )
                        }
                        ?: stringResource(id = BitwardenString.pam_slot_held_message),
                        leadingContent = { PamStateIcon(iconRes = BitwardenDrawable.ic_clock) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tag = "PamSlotHeldWarning"),
                    )
                } else {
                    BitwardenActionCard(
                        cardTitle = stringResource(id = BitwardenString.pam_can_request_access),
                        cardSubtitle = stringResource(id = BitwardenString.pam_credentials_masked),
                        leadingContent = { PamStateIcon(iconRes = BitwardenDrawable.ic_locked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tag = "PamBanner"),
                    )
                }
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.pam_request_access),
                    onClick = onRequestAccessClick,
                    isEnabled = !pamState.isSlotHeldByAnotherMember,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(tag = "PamRequestAccessButton"),
                )
            }

            VaultItemPamState.Pending -> {
                BitwardenActionCard(
                    cardTitle = stringResource(id = BitwardenString.pam_access_pending),
                    cardSubtitle = stringResource(id = BitwardenString.pam_access_pending_message),
                    leadingContent = { PamStateIcon(iconRes = BitwardenDrawable.ic_clock) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "PamBanner"),
                )
            }

            is VaultItemPamState.Approved -> {
                BitwardenActionCard(
                    cardTitle = stringResource(id = BitwardenString.pam_access_approved),
                    cardSubtitle = stringResource(id = BitwardenString.pam_access_approved_message),
                    leadingContent = { PamStateIcon(iconRes = BitwardenDrawable.ic_check_mark) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "PamBanner"),
                )
                if (pamState.requestId != null) {
                    BitwardenFilledButton(
                        label = stringResource(id = BitwardenString.pam_start_access),
                        onClick = onStartAccessClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(tag = "PamStartAccessButton"),
                    )
                    BitwardenOutlinedButton(
                        label = stringResource(id = BitwardenString.pam_cancel_request),
                        onClick = onCancelRequestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag(tag = "PamCancelRequestButton"),
                    )
                }
            }

            is VaultItemPamState.Active -> {
                val isEnding = pamState.remainingMinutes <= ALERT_THRESHOLD_MINUTES
                BitwardenActionCard(
                    cardTitle = stringResource(id = BitwardenString.pam_access_active_title),
                    cardSubtitle = stringResource(id = BitwardenString.pam_access_active_message),
                    leadingContent = { PamStateIcon(iconRes = BitwardenDrawable.ic_unlocked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "PamBanner"),
                )
                RemainingTime(
                    remainingMinutes = pamState.remainingMinutes,
                    totalMinutes = pamState.totalMinutes,
                    isEnding = isEnding,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * The leading icon of the access state card.
 */
@Composable
private fun PamStateIcon(iconRes: Int) {
    Icon(
        painter = rememberVectorPainter(id = iconRes),
        contentDescription = null,
        tint = BitwardenTheme.colorScheme.icon.secondary,
    )
}

/**
 * The remaining lease time as a progress bar that drains over the lease, with the time left as
 * text. The bar turns to the error color in the last few minutes.
 */
@Composable
private fun RemainingTime(
    remainingMinutes: Long,
    totalMinutes: Long,
    isEnding: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isEnding) {
        BitwardenTheme.colorScheme.status.error
    } else {
        BitwardenTheme.colorScheme.filledButton.background
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                id = BitwardenString.pam_access_time_left,
                remainingText(remainingMinutes = remainingMinutes),
            ),
            style = BitwardenTheme.typography.titleSmall,
            color = if (isEnding) {
                BitwardenTheme.colorScheme.status.error
            } else {
                BitwardenTheme.colorScheme.text.primary
            },
            modifier = Modifier.testTag(tag = "PamRemainingTime"),
        )
        LinearProgressIndicator(
            progress = {
                if (totalMinutes > 0) {
                    (remainingMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)
                } else {
                    0f
                }
            },
            color = color,
            trackColor = BitwardenTheme.colorScheme.stroke.divider,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 8.dp)
                .testTag(tag = "PamRemainingProgress"),
        )
    }
}

@Composable
private fun remainingText(remainingMinutes: Long): String {
    val hours = (remainingMinutes / MINUTES_PER_HOUR).toInt()
    val minutes = (remainingMinutes % MINUTES_PER_HOUR).toInt()
    return when {
        hours > 0 && minutes > 0 -> stringResource(
            id = BitwardenString.hours_minutes_format,
            formatArgs = arrayOf(
                pluralStringResource(
                    id = BitwardenPlurals.hours_format,
                    count = hours,
                    formatArgs = arrayOf(hours),
                ),
                pluralStringResource(
                    id = BitwardenPlurals.minutes_format,
                    count = minutes,
                    formatArgs = arrayOf(minutes),
                ),
            ),
        )

        hours > 0 -> pluralStringResource(
            id = BitwardenPlurals.hours_format,
            count = hours,
            formatArgs = arrayOf(hours),
        )

        minutes > 0 -> pluralStringResource(
            id = BitwardenPlurals.minutes_format,
            count = minutes,
            formatArgs = arrayOf(minutes),
        )

        else -> stringResource(id = BitwardenString.pam_less_than_a_minute)
    }
}
