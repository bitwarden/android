package com.x8bit.bitwarden.ui.platform.feature.premium.plan.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus

/**
 * Returns the localized label string resource for a [PremiumSubscriptionStatus].
 */
@StringRes
fun PremiumSubscriptionStatus.labelRes(): Int = when (this) {
    PremiumSubscriptionStatus.ACTIVE -> BitwardenString.subscription_status_active
    PremiumSubscriptionStatus.CANCELED -> BitwardenString.subscription_status_canceled
    PremiumSubscriptionStatus.PENDING_CANCELLATION -> {
        BitwardenString.subscription_status_pending_cancellation
    }

    PremiumSubscriptionStatus.PAST_DUE -> BitwardenString.subscription_status_past_due
    PremiumSubscriptionStatus.PAUSED -> BitwardenString.subscription_status_paused
    PremiumSubscriptionStatus.UPDATE_PAYMENT -> BitwardenString.subscription_status_update_payment
}

/**
 * Returns the [BitwardenColorScheme.StatusBadgeVariantColors] used to render the badge for a
 * [PremiumSubscriptionStatus].
 */
@Composable
fun PremiumSubscriptionStatus.badgeColors(): BitwardenColorScheme.StatusBadgeVariantColors =
    when (this) {
        PremiumSubscriptionStatus.ACTIVE -> BitwardenTheme.colorScheme.statusBadge.success
        PremiumSubscriptionStatus.CANCELED -> BitwardenTheme.colorScheme.statusBadge.error
        PremiumSubscriptionStatus.PAST_DUE,
        PremiumSubscriptionStatus.PAUSED,
        PremiumSubscriptionStatus.PENDING_CANCELLATION,
        PremiumSubscriptionStatus.UPDATE_PAYMENT,
            -> BitwardenTheme.colorScheme.statusBadge.warning
    }
