package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Models the result of fetching the user's premium subscription details.
 */
sealed class SubscriptionResult {
    /**
     * Subscription details were fetched successfully.
     */
    data class Success(
        val subscription: SubscriptionInfo,
    ) : SubscriptionResult()

    /**
     * An error occurred while fetching subscription details.
     */
    data class Error(
        val error: Throwable,
    ) : SubscriptionResult()
}
