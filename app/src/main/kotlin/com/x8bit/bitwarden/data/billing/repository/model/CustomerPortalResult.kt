package com.x8bit.bitwarden.data.billing.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

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
     * Generic error while retrieving the customer portal URL. The optional [errorMessage] may
     * be displayed directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : CustomerPortalResult()
}
