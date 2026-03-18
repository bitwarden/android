package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Models the result of creating a Stripe checkout session.
 */
sealed class CheckoutSessionResult {

    /**
     * The checkout session URL was successfully retrieved.
     */
    data class Success(
        val url: String,
    ) : CheckoutSessionResult()

    /**
     * There was an error creating the checkout session.
     */
    data class Error(
        val message: String?,
        val error: Throwable?,
    ) : CheckoutSessionResult()
}
