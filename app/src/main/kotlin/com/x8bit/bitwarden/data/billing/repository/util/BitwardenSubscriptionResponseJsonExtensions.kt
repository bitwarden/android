package com.x8bit.bitwarden.data.billing.repository.util

import com.bitwarden.network.model.BitwardenDiscountJson
import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.DiscountTypeJson
import com.bitwarden.network.model.SubscriptionStatusJson
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import java.math.BigDecimal
import java.math.RoundingMode

private val PERCENT_DIVISOR: BigDecimal = BigDecimal("100")
private const val MONEY_SCALE: Int = 2

/**
 * Maps a [BitwardenSubscriptionResponseJson] into a [SubscriptionInfo] domain
 * model.
 *
 * `discountAmount` is resolved at mapping time: fixed-amount discounts pass
 * through as-is; percent-off discounts apply to the password manager subtotal
 * (`seatsCost + storageCost`). `nextChargeTotal` is computed client-side as
 * `seatsCost + storageCost - discountAmount + estimatedTax` because the server
 * does not expose a precomputed total.
 */
fun BitwardenSubscriptionResponseJson.toSubscriptionInfo(): SubscriptionInfo {
    val seatsCost = cart.passwordManager.seats.cost
    val storageCost = cart.passwordManager.additionalStorage?.cost
    val discountAmount = cart.discount?.toMoneyAmount(
        subtotal = seatsCost + (storageCost ?: BigDecimal.ZERO),
    )
    val estimatedTax = cart.estimatedTax
    val nextChargeTotal = seatsCost +
        (storageCost ?: BigDecimal.ZERO) -
        (discountAmount ?: BigDecimal.ZERO) +
        estimatedTax

    return SubscriptionInfo(
        status = status.toPremiumSubscriptionStatus(),
        cadence = cart.cadence.toPlanCadence(),
        seatsCost = seatsCost,
        storageCost = storageCost,
        discountAmount = discountAmount,
        estimatedTax = estimatedTax,
        nextChargeTotal = nextChargeTotal,
        nextCharge = nextCharge,
        canceledDate = canceled,
        suspensionDate = suspension,
        gracePeriodDays = gracePeriod,
    )
}

private fun SubscriptionStatusJson.toPremiumSubscriptionStatus(): PremiumSubscriptionStatus =
    when (this) {
        SubscriptionStatusJson.ACTIVE,
        SubscriptionStatusJson.TRIALING,
        -> PremiumSubscriptionStatus.ACTIVE

        SubscriptionStatusJson.CANCELED,
        SubscriptionStatusJson.INCOMPLETE_EXPIRED,
        -> PremiumSubscriptionStatus.CANCELED

        SubscriptionStatusJson.INCOMPLETE,
        SubscriptionStatusJson.UNPAID,
        -> PremiumSubscriptionStatus.OVERDUE_PAYMENT

        SubscriptionStatusJson.PAST_DUE -> PremiumSubscriptionStatus.PAST_DUE

        SubscriptionStatusJson.PAUSED -> PremiumSubscriptionStatus.PAUSED
    }

private fun CadenceTypeJson.toPlanCadence(): PlanCadence = when (this) {
    CadenceTypeJson.ANNUALLY -> PlanCadence.ANNUALLY
    CadenceTypeJson.MONTHLY -> PlanCadence.MONTHLY
}

private fun BitwardenDiscountJson.toMoneyAmount(subtotal: BigDecimal): BigDecimal =
    when (type) {
        DiscountTypeJson.AMOUNT_OFF -> value
        DiscountTypeJson.PERCENT_OFF ->
            subtotal
                .multiply(value)
                .divide(PERCENT_DIVISOR, MONEY_SCALE, RoundingMode.HALF_EVEN)
    }
