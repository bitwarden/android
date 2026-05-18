package com.x8bit.bitwarden.data.billing.manager

import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionStatusState
import kotlinx.coroutines.flow.StateFlow

/**
 * URL opened when the user taps the "Learn more" CTA on the "Upgraded to Premium" action card.
 */
const val UPGRADED_TO_PREMIUM_LEARN_MORE_URL: String =
    "https://bitwarden.com/help/password-manager-plans/"

/**
 * Manages Premium upgrade state for the active user.
 */
interface PremiumStateManager {

    /**
     * Emits `true` when the current user is eligible to see the Premium upgrade banner,
     * or `false` otherwise. A user is "effectively premium" — and therefore ineligible
     * for the banner — only when their account is premium and their subscription is not
     * in a recovery or terminal state. Trouble states (past due, update payment, canceled,
     * paused) flip eligibility back on even while the server still reports
     * `isPremium=true` during the grace window.
     */
    val isPremiumUpgradeBannerEligibleFlow: StateFlow<Boolean>

    /**
     * Emits `true` while the active user is eligible to see the "Upgraded to Premium" action
     * card and `false` otherwise. Eligibility persists across app launches until the user
     * consumes the card via [dismissUpgradedToPremiumCard].
     */
    val isUpgradedToPremiumCardEligibleFlow: StateFlow<Boolean>

    /**
     * Emits `true` when the active user is eligible to see the Plan row in Settings, or `false`
     * otherwise.
     */
    val isPlanRowEligibleFlow: StateFlow<Boolean>

    /**
     * Emits the active user's latest [SubscriptionStatusState]. Fetches whenever there is
     * an active user (regardless of `Account.isPremium`) so that users whose Stripe
     * subscription has moved to a terminal state still surface the correct substate; emits
     * [SubscriptionStatusState.NoSubscription] when there is no active user or when the
     * server returns 404 (no `GatewaySubscriptionId`).
     */
    val subscriptionStatusStateFlow: StateFlow<SubscriptionStatusState>
    val subscriptionStatusStateFlow: StateFlow<SubscriptionStatusState>

    /**
     * Returns `true` when the in-app upgrade flow is available, or `false` otherwise.
     */
    fun isInAppUpgradeAvailable(): Boolean

    /**
     * Marks the Premium upgrade banner as dismissed for the current user.
     */
    fun dismissPremiumUpgradeBanner()

    /**
     * Marks the "Upgraded to Premium" action card as consumed for the current user. This is
     * called for both the dismiss (X) and Learn more interactions — once consumed, the card
     * never re-appears for that user.
     */
    fun dismissUpgradedToPremiumCard()
}
