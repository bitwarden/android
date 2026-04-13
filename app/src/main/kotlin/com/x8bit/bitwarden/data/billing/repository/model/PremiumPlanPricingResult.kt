package com.x8bit.bitwarden.data.billing.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models the result of retrieving premium plan pricing.
 */
sealed class PremiumPlanPricingResult {

    /**
     * The premium plan pricing was successfully retrieved.
     *
     * @property annualPrice The annual price in the plan's currency.
     */
    data class Success(
        val annualPrice: Double,
    ) : PremiumPlanPricingResult()

    /**
     * An error occurred while retrieving the premium plan pricing.
     * The optional [errorMessage] may be displayed directly in the UI
     * when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : PremiumPlanPricingResult()
}
