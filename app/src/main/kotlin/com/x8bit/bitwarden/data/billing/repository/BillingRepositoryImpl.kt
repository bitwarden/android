package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.model.GetSubscriptionResponse
import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import com.x8bit.bitwarden.data.billing.repository.util.toSubscriptionInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The default implementation of [BillingRepository].
 */
class BillingRepositoryImpl(
    playBillingManager: PlayBillingManager,
    private val billingService: BillingService,
) : BillingRepository {

    private val mutableSubscriptionResultFlow = MutableSharedFlow<SubscriptionResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

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
                onSuccess = { response ->
                    when (response) {
                        is GetSubscriptionResponse.Success -> SubscriptionResult.Success(
                            subscription = response.subscription.toSubscriptionInfo(),
                        )

                        is GetSubscriptionResponse.NotFound -> SubscriptionResult.NotFound
                    }
                },
                onFailure = { SubscriptionResult.Error(error = it) },
            )
            .also { mutableSubscriptionResultFlow.emit(it) }

    override fun getSubscriptionFlow(): Flow<SubscriptionResult> = mutableSubscriptionResultFlow
}
