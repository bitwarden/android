package com.x8bit.bitwarden.data.billing.repository.model

/**
 * Represents the UI-facing subscription status for premium users.
 */
enum class PremiumSubscriptionStatus {
    ACTIVE,
    CANCELED,

    /**
     * The subscription's initial payment never succeeded and Stripe voided the invoice, so
     * the subscription never became active. Distinct from [CANCELED], which describes a
     * subscription that was previously active.
     */
    EXPIRED,

    /**
     * The subscription is scheduled to cancel at a future date but is still active until then.
     */
    PENDING_CANCELLATION,
    PAST_DUE,
    PAUSED,

    /**
     * The subscription is delinquent past the grace period and Stripe has stopped attempting
     * to collect payment. Recoverable only by resolving the outstanding invoices.
     */
    UNPAID,
    UPDATE_PAYMENT,
}
