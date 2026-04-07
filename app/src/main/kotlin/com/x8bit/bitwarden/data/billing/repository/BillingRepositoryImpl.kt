package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import kotlinx.coroutines.flow.StateFlow

/**
 * The default implementation of [BillingRepository].
 */
class BillingRepositoryImpl(
    playBillingManager: PlayBillingManager,
    private val billingService: BillingService,
) : BillingRepository {

    override val isInAppBillingSupportedFlow: StateFlow<Boolean> =
        playBillingManager.isInAppBillingSupportedFlow

    override suspend fun getCheckoutSessionUrl(): CheckoutSessionResult =
        billingService
            .createCheckoutSession()
            .fold(
                onSuccess = { CheckoutSessionResult.Success(url = it.checkoutSessionUrl) },
                onFailure = { CheckoutSessionResult.Error(error = it) },
            )

    override suspend fun getPortalUrl(): CustomerPortalResult =
        billingService
            .getPortalUrl()
            .fold(
                onSuccess = { CustomerPortalResult.Success(url = it.url) },
                onFailure = { CustomerPortalResult.Error(error = it) },
            )
}
