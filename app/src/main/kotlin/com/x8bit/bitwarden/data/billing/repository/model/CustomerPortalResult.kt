package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Models the result of retrieving the Stripe customer portal URL.
 */
sealed class CustomerPortalResult {

    /**
     * The customer portal URL was successfully retrieved.
     */
    data class Success(
        val url: String,
    ) : CustomerPortalResult()

    /**
     * There was an error retrieving the customer portal URL.
     */
    data class Error(
        val message: String?,
        val error: Throwable?,
    ) : CustomerPortalResult()
}
