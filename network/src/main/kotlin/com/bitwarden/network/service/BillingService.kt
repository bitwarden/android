package com.bitwarden.network.service

import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.PortalUrlResponseJson
import com.bitwarden.network.model.PremiumPlanResponseJson

/**
 * Provides an API for interacting with the billing endpoints.
 */
interface BillingService {

    /**
     * Creates a Stripe checkout session for Premium upgrade.
     */
    suspend fun createCheckoutSession(): Result<CheckoutSessionResponseJson>

    /**
     * Creates a Stripe customer portal session for managing the Premium subscription.
     */
    suspend fun getPortalUrl(): Result<PortalUrlResponseJson>

    /**
     * Retrieves the premium plan pricing information.
     */
    suspend fun getPremiumPlan(): Result<PremiumPlanResponseJson>

    /**
     * Retrieves the user's premium subscription details.
     */
    suspend fun getSubscription(): Result<BitwardenSubscriptionResponseJson>
}
