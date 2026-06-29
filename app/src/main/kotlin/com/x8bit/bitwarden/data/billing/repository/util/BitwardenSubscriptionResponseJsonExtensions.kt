package com.x8bit.bitwarden.data.billing.repository.util

import com.bitwarden.network.model.BitwardenDiscountJson
import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.CartItemJson
import com.bitwarden.network.model.DiscountTypeJson
import com.bitwarden.network.model.SubscriptionStatusJson
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import java.math.BigDecimal
import java.math.RoundingMode

private const val MONEY_SCALE: Int = 2

/**
 * Maps a [BitwardenSubscriptionResponseJson] into a [SubscriptionInfo] domain
 * model.
 *
 * Each line item's `cost` is a per-unit price, so its contribution is
 * `cost * quantity`. Two discount channels are combined into `discountAmount`:
 * the cart-level discount applies to the password manager subtotal
 * (`seatsCost + storageCost`), and the Password Manager seats item-level
 * discount applies to the seats line total. Item-level discounts on other line
 * items are intentionally ignored, mirroring the web client. Fixed-amount
 * discounts pass through as-is; percent-off discounts treat a value below 1 as
 * an already-decimal fraction and round half-up. `nextChargeTotal` is computed
 * client-side as `subtotal - discountAmount + estimatedTax` because the server
 * does not expose a precomputed total.
 */
fun BitwardenSubscriptionResponseJson.toSubscriptionInfo(): SubscriptionInfo {
    val seatsCost = cart.passwordManager.seats.lineTotal()
    val storageCost = cart.passwordManager.additionalStorage?.lineTotal()
    val subtotal = seatsCost + (storageCost ?: BigDecimal.ZERO)
    val cartDiscount = cart.discount?.toDiscountAmount(baseAmount = subtotal)
    val seatsDiscount = cart.passwordManager.seats.discount
        ?.toDiscountAmount(baseAmount = seatsCost)
    val discountAmount = listOfNotNull(cartDiscount, seatsDiscount)
        .takeIf { it.isNotEmpty() }
        ?.reduce(BigDecimal::add)
    val estimatedTax = cart.estimatedTax
    val nextChargeTotal = subtotal -
        (discountAmount ?: BigDecimal.ZERO) +
        estimatedTax

    return SubscriptionInfo(
        status = toPremiumSubscriptionStatus(),
        cadence = cart.cadence.toPlanCadence(),
        seatsCost = seatsCost,
        storageCost = storageCost,
        discountAmount = discountAmount,
        estimatedTax = estimatedTax,
        nextChargeTotal = nextChargeTotal,
        nextCharge = nextCharge,
        cancelAt = cancelAt,
        canceledDate = canceled,
        suspensionDate = suspension,
        gracePeriodDays = gracePeriod,
    )
}

private fun BitwardenSubscriptionResponseJson.toPremiumSubscriptionStatus():
    PremiumSubscriptionStatus = when (status) {
    SubscriptionStatusJson.ACTIVE,
    SubscriptionStatusJson.TRIALING,
        -> {
        if (cancelAt != null) {
            PremiumSubscriptionStatus.PENDING_CANCELLATION
        } else {
            PremiumSubscriptionStatus.ACTIVE
        }
    }

    SubscriptionStatusJson.CANCELED -> PremiumSubscriptionStatus.CANCELED
    SubscriptionStatusJson.INCOMPLETE_EXPIRED -> PremiumSubscriptionStatus.EXPIRED
    SubscriptionStatusJson.INCOMPLETE -> PremiumSubscriptionStatus.UPDATE_PAYMENT
    SubscriptionStatusJson.UNPAID -> PremiumSubscriptionStatus.UNPAID
    SubscriptionStatusJson.PAST_DUE -> PremiumSubscriptionStatus.PAST_DUE
    SubscriptionStatusJson.PAUSED -> PremiumSubscriptionStatus.PAUSED
}

private fun CartItemJson.lineTotal(): BigDecimal = cost.multiply(quantity.toBigDecimal())

private fun CadenceTypeJson.toPlanCadence(): PlanCadence = when (this) {
    CadenceTypeJson.ANNUALLY -> PlanCadence.ANNUALLY
    CadenceTypeJson.MONTHLY -> PlanCadence.MONTHLY
}

private fun BitwardenDiscountJson.toDiscountAmount(baseAmount: BigDecimal): BigDecimal =
    when (type) {
        DiscountTypeJson.AMOUNT_OFF -> value
        DiscountTypeJson.PERCENT_OFF -> {
            val percentage = if (value < BigDecimal.ONE) value else value.movePointLeft(2)
            baseAmount.multiply(percentage).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        }
    }
