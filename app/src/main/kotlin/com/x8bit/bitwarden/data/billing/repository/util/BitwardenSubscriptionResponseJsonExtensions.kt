package com.x8bit.bitwarden.data.billing.repository.util

import com.bitwarden.network.model.BitwardenSubscriptionResponseJson
import com.bitwarden.network.model.CadenceTypeJson
import com.bitwarden.network.model.InvoicePreviewItemJson
import com.bitwarden.network.model.SubscriptionStatusJson
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import java.math.BigDecimal

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
    val seatsCost = invoicePreview.passwordManager.seats?.lineTotal() ?: BigDecimal.ZERO
    val storageCost = invoicePreview.passwordManager.additionalStorage?.lineTotal()
    val discountAmount = invoicePreview
        .discounts
        ?.map { it.amount }
        ?.reduce(operation = BigDecimal::add)

    return SubscriptionInfo(
        status = toPremiumSubscriptionStatus(),
        cadence = invoicePreview.cadence.toPlanCadence(),
        seatsCost = seatsCost,
        storageCost = storageCost,
        discountAmount = discountAmount,
        estimatedTax = invoicePreview.estimatedTax,
        nextChargeTotal = invoicePreview.total,
        nextCharge = invoicePreview.nextPaymentAttempt,
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
}

private fun InvoicePreviewItemJson.lineTotal(): BigDecimal = cost.multiply(quantity.toBigDecimal())

private fun CadenceTypeJson.toPlanCadence(): PlanCadence = when (this) {
    CadenceTypeJson.ANNUALLY -> PlanCadence.ANNUALLY
    CadenceTypeJson.MONTHLY -> PlanCadence.MONTHLY
}
