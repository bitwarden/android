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
     * Emits the latest "is the environment effectively self-hosted for premium upgrade gating"
     * value. Mirrors [isSelfHosted] but reacts to both environment changes and toggles of the
     * [com.bitwarden.core.data.manager.model.FlagKey.DebugDisableSelfHostPremiumCheck] debug
     * flag, so consumers stay in sync when QA flips the override at runtime.
     */
    val isSelfHostedFlow: StateFlow<Boolean>

    /**
     * Returns `true` when the in-app upgrade flow is available, or `false` otherwise.
     */
    fun isInAppUpgradeAvailable(): Boolean

    /**
     * Returns `true` when the current environment is effectively self-hosted for
     * premium upgrade gating. Returns `false` for cloud environments and for
     * self-hosted environments when [com.bitwarden.core.data.manager.model.FlagKey.DebugDisableSelfHostPremiumCheck]
     * is enabled (QA bypass for testing premium flows on internal self-hosted envs).
     */
    fun isSelfHosted(): Boolean

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
