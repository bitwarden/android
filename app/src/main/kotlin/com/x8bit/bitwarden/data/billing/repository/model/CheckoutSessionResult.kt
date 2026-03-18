package com.x8bit.bitwarden.data.billing.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

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
     * Generic error while creating a checkout session. The optional [errorMessage] may be
     * displayed directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : CheckoutSessionResult()
}
