package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Represents the UI-facing subscription status for premium users.
 */
enum class PremiumSubscriptionStatus {
    ACTIVE,
    CANCELED,
    OVERDUE_PAYMENT,
    PAST_DUE,
    PAUSED,
}
