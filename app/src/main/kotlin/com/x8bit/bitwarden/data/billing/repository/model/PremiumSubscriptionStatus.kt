package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Represents the UI-facing subscription status for premium users.
 */
enum class PremiumSubscriptionStatus {
    ACTIVE,
    CANCELED,

    /**
     * The subscription is scheduled to cancel at a future date but is still active until then.
     */
    PENDING_CANCELLATION,
    PAST_DUE,
    PAUSED,
    UPDATE_PAYMENT,
}
