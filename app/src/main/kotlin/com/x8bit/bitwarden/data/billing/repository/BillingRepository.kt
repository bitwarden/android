package com.x8bit.bitwarden.data.billing.repository

import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for managing billing operations.
 */
interface BillingRepository {

    /**
     * Emits `true` when in-app billing is supported, or `false` otherwise.
     */
    val isInAppBillingSupportedFlow: StateFlow<Boolean>

    /**
     * Creates a Stripe checkout session and returns the checkout URL.
     */
    suspend fun getCheckoutSessionUrl(): CheckoutSessionResult

    /**
     * Retrieves the Stripe customer portal URL for managing the Premium subscription.
     */
    suspend fun getPortalUrl(): CustomerPortalResult
}
