package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult

/**
 * The default implementation of [BillingRepository].
 */
class BillingRepositoryImpl(
    private val billingService: BillingService,
) : BillingRepository {

    override suspend fun getCheckoutSessionUrl(): CheckoutSessionResult =
        billingService
            .createCheckoutSession()
            .fold(
                onSuccess = { CheckoutSessionResult.Success(url = it.checkoutSessionUrl) },
                onFailure = {
                    CheckoutSessionResult.Error(
                        message = it.message,
                        error = it,
                    )
                },
            )

    override suspend fun getPortalUrl(): CustomerPortalResult =
        billingService
            .getPortalUrl()
            .fold(
                onSuccess = { CustomerPortalResult.Success(url = it.url) },
                onFailure = {
                    CustomerPortalResult.Error(
                        message = it.message,
                        error = it,
                    )
                },
            )
}
