package com.x8bit.bitwarden.data.billing.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages interactions with the Google Play Billing system.
 */
interface PlayBillingManager {

    /**
     * Emits `true` when in-app billing is supported, or `false` otherwise.
     */
    val isInAppBillingSupportedFlow: StateFlow<Boolean>
}
