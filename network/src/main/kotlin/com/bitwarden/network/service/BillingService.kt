package com.bitwarden.network.service

import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.PortalUrlResponseJson

/**
 * Provides an API for interacting with the billing endpoints.
 */
interface BillingService {

    /**
     * Creates a Stripe checkout session for premium upgrade.
     */
    suspend fun createCheckoutSession(): Result<CheckoutSessionResponseJson>

    /**
     * Creates a Stripe customer portal session for managing the premium subscription.
     */
    suspend fun getPortalUrl(): Result<PortalUrlResponseJson>
}
