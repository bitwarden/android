package com.x8bit.bitwarden.data.billing.repository

import com.bitwarden.network.service.BillingService
import com.x8bit.bitwarden.data.billing.manager.PlayBillingManager
import com.x8bit.bitwarden.data.billing.repository.model.CheckoutSessionResult
import com.x8bit.bitwarden.data.billing.repository.model.CustomerPortalResult
import com.x8bit.bitwarden.data.billing.repository.model.PremiumPlanPricingResult
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.Locale

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
                    val monthlyPrice = it.seat.price / MONTHS_PER_YEAR
                    val formatted = NumberFormat
                        .getCurrencyInstance(Locale.US)
                        .format(monthlyPrice)
                    PremiumPlanPricingResult.Success(
                        monthlyRate = formatted,
                    )
                },
                onFailure = {
                    PremiumPlanPricingResult.Error(error = it)
                },
            )
}

private const val MONTHS_PER_YEAR = 12
