package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Latest observed substate of the active user's premium subscription.
 *
 * The subscription endpoint is only meaningful for users who have a `GatewaySubscriptionId`
 * on the server, so [NoSubscription] is emitted both for users we never queried (no personal
 * premium signal) and for users whose fetch returned 404. [Error] preserves the failure for
 * retry, while [Available] surfaces the raw status so consumers can apply their own policy.
 */
sealed class SubscriptionStatusState {

    /**
     * No fetch has been attempted yet for the active user.
     */
    data object Loading : SubscriptionStatusState()

    /**
     * The active user has no recorded premium subscription.
     */
    data object NoSubscription : SubscriptionStatusState()

    /**
     * The active user has a subscription with the given [status].
     */
    data class Available(
        val status: PremiumSubscriptionStatus,
    ) : SubscriptionStatusState()

    /**
     * The fetch failed for a reason other than 404.
     */
    data class Error(
        val throwable: Throwable,
    ) : SubscriptionStatusState()
}
