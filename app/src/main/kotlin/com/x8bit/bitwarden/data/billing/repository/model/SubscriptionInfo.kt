package com.x8bit.bitwarden.data.billing.repository.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Domain model containing a premium subscription's billing and lifecycle details.
 *
 * @property status The UI-facing subscription status.
 * @property cadence The billing cadence (annual or monthly).
 * @property seatsCost The cost of the seat line item for the current cadence.
 * @property storageCost The cost of additional storage, or null if none.
 * @property discountAmount The money value of any applied discount, or null if no discount is
 * present. Percent-off discounts are resolved against the password manager subtotal at mapping
 * time.
 * @property estimatedTax The estimated tax charged on the next invoice.
 * @property nextChargeTotal The total of the next invoice:
 * `seatsCost + (storageCost ?: 0) - (discountAmount ?: 0) + estimatedTax`.
 * @property nextCharge The date of the next charge, or null if not applicable.
 * @property canceledDate The date the subscription was canceled, or null.
 * @property suspensionDate The date the subscription will be suspended, or null.
 * @property gracePeriodDays The grace period in days, or null.
 */
data class SubscriptionInfo(
    val status: PremiumSubscriptionStatus,
    val cadence: PlanCadence,
    val seatsCost: BigDecimal,
    val storageCost: BigDecimal?,
    val discountAmount: BigDecimal?,
    val estimatedTax: BigDecimal,
    val nextChargeTotal: BigDecimal,
    val nextCharge: Instant?,
    val canceledDate: Instant?,
    val suspensionDate: Instant?,
    val gracePeriodDays: Int?,
)
