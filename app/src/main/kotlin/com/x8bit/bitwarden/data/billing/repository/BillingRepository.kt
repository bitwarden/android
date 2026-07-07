package com.x8bit.bitwarden.data.billing.repository

import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import kotlinx.coroutines.flow.Flow
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

    /**
     * Retrieves the premium plan pricing information.
     */
    suspend fun getPremiumPlanPricing(): PremiumPlanPricingResult

    /**
     * Fetches the current user's premium subscription details. The endpoint 404s when the
     * user has no `GatewaySubscriptionId` (free user); callers receive
     * [SubscriptionResult.NotFound] in that case instead of [SubscriptionResult.Error].
     */
    suspend fun getSubscription(): SubscriptionResult

    /**
     * A flow that emits the result of every [getSubscription] call. New collectors receive nothing
     * until the next [getSubscription] invocation rather than the most recent result.
     */
    fun getSubscriptionFlow(): Flow<SubscriptionResult>
}
