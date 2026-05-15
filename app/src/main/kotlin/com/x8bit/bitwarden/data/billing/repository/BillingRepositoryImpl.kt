package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.model.BitwardenError
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import com.x8bit.bitwarden.data.billing.repository.util.toSubscriptionInfo
import kotlinx.coroutines.flow.StateFlow

private const val HTTP_CODE_NOT_FOUND: Int = 404

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

    override suspend fun getPremiumPlanPricing(): PremiumPlanPricingResult =
        billingService
            .getPremiumPlan()
            .fold(
                onSuccess = {
                    PremiumPlanPricingResult.Success(
                        annualPrice = it.seat.price,
                    )
                },
                onFailure = {
                    PremiumPlanPricingResult.Error(error = it)
                },
            )

    override suspend fun getSubscription(): SubscriptionResult =
        billingService
            .getSubscription()
            .fold(
                onSuccess = {
                    SubscriptionResult.Success(
                        subscription = it.toSubscriptionInfo(),
                    )
                },
                onFailure = { throwable ->
                    val bitwardenError = throwable.toBitwardenError()
                    if (bitwardenError is BitwardenError.Http &&
                        bitwardenError.code == HTTP_CODE_NOT_FOUND
                    ) {
                        SubscriptionResult.NotFound
                    } else {
                        SubscriptionResult.Error(error = throwable)
                    }
                },
            )
}
