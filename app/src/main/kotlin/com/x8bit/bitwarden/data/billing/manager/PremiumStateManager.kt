package com.x8bit.bitwarden.data.billing.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the consolidated eligibility state for the Premium upgrade banner.
 *
 * Combines multiple upstream signals (Premium status, billing support, feature flag,
 * banner dismissal, account age, and vault item count) into a single observable flow.
 */
interface PremiumStateManager {

    /**
     * Emits `true` when the current user is eligible to see the Premium upgrade banner,
     * or `false` otherwise.
     */
    val isPremiumUpgradeBannerEligibleFlow: StateFlow<Boolean>

    /**
     * Marks the Premium upgrade banner as dismissed for the current user.
     */
    fun dismissPremiumUpgradeBanner()
}
