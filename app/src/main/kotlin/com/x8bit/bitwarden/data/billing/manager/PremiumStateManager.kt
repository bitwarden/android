package com.x8bit.bitwarden.data.billing.manager

import kotlinx.coroutines.flow.StateFlow

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
     * Returns `true` when the in-app upgrade flow is available, or `false` otherwise.
     */
    fun isInAppUpgradeAvailable(): Boolean

    /**
     * Marks the Premium upgrade banner as dismissed for the current user.
     */
    fun dismissPremiumUpgradeBanner()
}
