package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.service.BillingService

/**
 * The default implementation of [BillingRepository].
 */
class BillingRepositoryImpl(
    private val billingService: BillingService,
) : BillingRepository {

    override suspend fun getCheckoutSessionUrl(): Result<String> =
        billingService
            .createCheckoutSession()
            .map { it.checkoutSessionUrl }

    override suspend fun getPortalUrl(): Result<String> =
        billingService
            .getPortalUrl()
            .map { it.url }
}
