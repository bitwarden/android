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
     * The endpoint returned 404, indicating the user has no subscription on record
     * (e.g., the active account has never had a Stripe `GatewaySubscriptionId`).
     * Consumers should treat this as a free user.
     */
    data object NotFound : SubscriptionResult()

    /**
     * An error occurred while fetching subscription details.
     */
    data class Error(
        val error: Throwable,
    ) : SubscriptionResult()
}
