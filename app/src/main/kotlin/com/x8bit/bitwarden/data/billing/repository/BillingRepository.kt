package com.x8bit.bitwarden.data.billing.repository

import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult

/**
 * Provides an API for managing billing operations.
 */
interface BillingRepository {

    /**
     * Creates a Stripe checkout session and returns the checkout URL.
     */
    suspend fun getCheckoutSessionUrl(): CheckoutSessionResult

    /**
     * Retrieves the Stripe customer portal URL for managing the premium subscription.
     */
    suspend fun getPortalUrl(): CustomerPortalResult
}
