package com.x8bit.bitwarden.data.billing.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models the result of retrieving premium plan pricing.
 */
sealed class PremiumPlanPricingResult {

    /**
     * The premium plan pricing was successfully retrieved.
     *
     * @property monthlyRate The formatted monthly rate (e.g. "$1.67").
     */
    data class Success(
        val monthlyRate: String,
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
