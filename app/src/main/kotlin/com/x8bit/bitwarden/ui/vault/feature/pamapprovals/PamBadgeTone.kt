package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bitwarden.ui.platform.components.badge.BitwardenStatusBadge
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus

/**
 * The tone of a privileged access state badge, mapped onto the design system badge variants.
 */
enum class PamBadgeTone {
    /**
     * Waiting on someone, such as a request that is pending approval.
     */
    WARNING,

    /**
     * A positive outcome, such as an approved request.
     */
    SUCCESS,

    /**
     * Access that is in progress, such as an active lease.
     */
    INFO,

    /**
     * A request or lease that did not or no longer grants access.
     */
    ERROR,
}

/**
 * The tone used to badge a request in this [PamRequestStatus].
 */
fun PamRequestStatus.badgeTone(): PamBadgeTone = when (this) {
    PamRequestStatus.PENDING -> PamBadgeTone.WARNING
    PamRequestStatus.APPROVED -> PamBadgeTone.SUCCESS
    PamRequestStatus.DENIED,
    PamRequestStatus.CANCELLED,
    PamRequestStatus.EXPIRED,
        -> PamBadgeTone.ERROR

    PamRequestStatus.UNKNOWN -> PamBadgeTone.INFO
}

/**
 * Displays the [label] of a privileged access request or lease state as a badge in the color for
 * the given [tone].
 */
@Composable
fun PamStatusBadge(
    label: String,
    tone: PamBadgeTone,
    modifier: Modifier = Modifier,
) {
    BitwardenStatusBadge(
        label = label,
        colors = tone.colors(),
        modifier = modifier,
    )
}

@Composable
private fun PamBadgeTone.colors(): BitwardenColorScheme.StatusBadgeVariantColors =
    when (this) {
        PamBadgeTone.WARNING -> BitwardenTheme.colorScheme.statusBadge.warning
        PamBadgeTone.SUCCESS -> BitwardenTheme.colorScheme.statusBadge.success
        PamBadgeTone.INFO -> BitwardenTheme.colorScheme.statusBadge.primary
        PamBadgeTone.ERROR -> BitwardenTheme.colorScheme.statusBadge.error
    }
