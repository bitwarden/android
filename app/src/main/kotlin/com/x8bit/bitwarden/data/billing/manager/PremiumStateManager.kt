package com.x8bit.bitwarden.data.billing.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the consolidated eligibility state for the premium upgrade banner.
 *
 * Combines multiple upstream signals (premium status, billing support, feature flag,
 * banner dismissal, account age, and vault item count) into a single observable flow.
 */
interface PremiumStateManager {

    /**
     * Emits `true` when the current user is eligible to see the premium upgrade banner,
     * or `false` otherwise.
     */
    val isPremiumUpgradeBannerEligibleFlow: StateFlow<Boolean>

    /**
     * Marks the premium upgrade banner as dismissed for the current user.
     */
    fun dismissPremiumUpgradeBanner()
}
