package com.bitwarden.network.api

import com.bitwarden.network.model.CheckoutSessionRequestJson
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.PortalUrlResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /account/billing API with authentication applied.
 */
internal interface AuthenticatedBillingApi {

    /**
     * Creates a Stripe checkout session for Premium upgrade.
     */
    @POST("/account/billing/vnext/premium/checkout")
    suspend fun createCheckoutSession(
        @Body body: CheckoutSessionRequestJson,
    ): NetworkResult<CheckoutSessionResponseJson>

    /**
     * Creates a Stripe customer portal session for managing the Premium subscription.
     */
    @POST("/account/billing/vnext/portal-session")
    suspend fun getPortalUrl(): NetworkResult<PortalUrlResponseJson>
}
