package com.bitwarden.network.service

import com.bitwarden.network.api.AuthenticatedBillingApi
import com.bitwarden.network.model.CheckoutSessionRequestJson
import com.bitwarden.network.model.CheckoutSessionResponseJson
import com.bitwarden.network.model.GetSubscriptionResponse
import com.bitwarden.network.model.PortalUrlResponseJson
import com.bitwarden.network.model.PremiumPlanResponseJson
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.util.NetworkErrorCode
import com.bitwarden.network.util.isErrorCode
import com.bitwarden.network.util.toResult

private const val PLATFORM = "android"

/**
 * The default implementation of the [BillingService].
 */
internal class BillingServiceImpl(
    private val authenticatedBillingApi: AuthenticatedBillingApi,
) : BillingService {

    override suspend fun createCheckoutSession(): Result<CheckoutSessionResponseJson> =
        authenticatedBillingApi
            .createCheckoutSession(
                body = CheckoutSessionRequestJson(platform = PLATFORM),
            )
            .toResult()

    override suspend fun getPortalUrl(): Result<PortalUrlResponseJson> =
        authenticatedBillingApi
            .getPortalUrl()
            .toResult()

    override suspend fun getPremiumPlan(): Result<PremiumPlanResponseJson> =
        authenticatedBillingApi
            .getPremiumPlan()
            .toResult()

    override suspend fun getSubscription(): Result<GetSubscriptionResponse> =
        authenticatedBillingApi
            .getSubscription()
            .toResult()
            .map { GetSubscriptionResponse.Success(subscription = it) }
            .recoverCatching { throwable ->
                val bitwardenError = throwable.toBitwardenError()
                if (bitwardenError.isErrorCode(code = NetworkErrorCode.NOT_FOUND)) {
                    GetSubscriptionResponse.NotFound(throwable = throwable)
                } else {
                    throw throwable
                }
            }
}
