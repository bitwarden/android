package com.x8bit.bitwarden.data.billing.manager

import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionStatusState
import com.x8bit.bitwarden.data.billing.repository.model.UpgradeLifecycleState
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
     * or `false` otherwise.
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
     * Emits the active user's latest [SubscriptionStatusState].
     */
    val subscriptionStatusStateFlow: StateFlow<SubscriptionStatusState>

    /**
     * Emits the active user's current [UpgradeLifecycleState].
     */
    val lifecycleStateFlow: StateFlow<UpgradeLifecycleState>

    /**
     * Emits whether the current state should be treated as self-hosted for premium upgrade
     * gating. Reactive equivalent of [isSelfHosted].
     */
    val isSelfHostedFlow: StateFlow<Boolean>

    /**
     * `true` when the current state should be treated as self-hosted for premium upgrade
     * gating, or `false` otherwise.
     */
    val isSelfHosted: Boolean

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

    /**
     * Marks the active user as having a Premium upgrade in flight (Stripe checkout completed
     * but the server has not yet flipped `isPremium`).
     */
    fun markPremiumUpgradePending(userId: String)
}
