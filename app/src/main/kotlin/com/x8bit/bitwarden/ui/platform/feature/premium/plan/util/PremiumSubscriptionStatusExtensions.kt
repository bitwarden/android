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
    PremiumSubscriptionStatus.EXPIRED -> BitwardenString.subscription_status_expired
    PremiumSubscriptionStatus.PENDING_CANCELLATION -> {
        BitwardenString.subscription_status_pending_cancellation
    }

    PremiumSubscriptionStatus.PAST_DUE -> BitwardenString.subscription_status_past_due
    PremiumSubscriptionStatus.PAUSED -> BitwardenString.subscription_status_paused
    PremiumSubscriptionStatus.UNPAID -> BitwardenString.subscription_status_unpaid
    PremiumSubscriptionStatus.UPDATE_PAYMENT -> BitwardenString.subscription_status_update_payment
}

/**
 * Returns `true` when the Premium plan card should replace its billing line items with the
 * premium feature list. Reserved for terminal states where line items carry no actionable
 * information and the user's path forward is to resubscribe.
 */
fun PremiumSubscriptionStatus.showsFeatureList(): Boolean = when (this) {
    PremiumSubscriptionStatus.CANCELED,
    PremiumSubscriptionStatus.EXPIRED,
        -> true

    PremiumSubscriptionStatus.ACTIVE,
    PremiumSubscriptionStatus.PAST_DUE,
    PremiumSubscriptionStatus.PAUSED,
    PremiumSubscriptionStatus.PENDING_CANCELLATION,
    PremiumSubscriptionStatus.UNPAID,
    PremiumSubscriptionStatus.UPDATE_PAYMENT,
        -> false
}

/**
 * Returns the [BitwardenColorScheme.StatusBadgeVariantColors] used to render the badge for a
 * [PremiumSubscriptionStatus].
 */
@Composable
fun PremiumSubscriptionStatus.badgeColors(): BitwardenColorScheme.StatusBadgeVariantColors =
    when (this) {
        PremiumSubscriptionStatus.ACTIVE -> BitwardenTheme.colorScheme.statusBadge.success
        PremiumSubscriptionStatus.CANCELED,
        PremiumSubscriptionStatus.EXPIRED,
        PremiumSubscriptionStatus.UNPAID,
            -> BitwardenTheme.colorScheme.statusBadge.error

        PremiumSubscriptionStatus.PAST_DUE,
        PremiumSubscriptionStatus.PAUSED,
        PremiumSubscriptionStatus.PENDING_CANCELLATION,
        PremiumSubscriptionStatus.UPDATE_PAYMENT,
            -> BitwardenTheme.colorScheme.statusBadge.warning
    }
