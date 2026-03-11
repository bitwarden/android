package com.x8bit.bitwarden.data.billing.repository

/**
 * Provides an API for managing billing operations.
 */
interface BillingRepository {

    /**
     * Creates a Stripe checkout session and returns the checkout URL.
     */
    suspend fun getCheckoutSessionUrl(): Result<String>

    /**
     * Retrieves the Stripe customer portal URL for managing the premium subscription.
     */
    suspend fun getPortalUrl(): Result<String>
}
