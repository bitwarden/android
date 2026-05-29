package com.x8bit.bitwarden.ui.platform.feature.premium.plan.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import java.math.BigDecimal
import java.text.NumberFormat

/**
 * Formats this amount as a cadence-qualified billing rate (e.g. "$10.00 per year"), using
 * [currencyFormatter] for the locale-aware currency value.
 */
fun BigDecimal.toBillingAmountText(
    cadence: PlanCadence,
    currencyFormatter: NumberFormat,
): Text {
    val formatted = currencyFormatter.format(this)
    val cadenceRes = when (cadence) {
        PlanCadence.ANNUALLY -> BitwardenString.billing_rate_per_year
        PlanCadence.MONTHLY -> BitwardenString.billing_rate_per_month
    }
    return cadenceRes.asText(formatted)
}

/**
 * Formats this amount for an always-rendered line item. Null is coerced to zero so the row still
 * shows the locale-formatted `$0.00`, as the Estimated Tax and Total rows always render.
 */
fun BigDecimal?.toRequiredMoneyText(currencyFormatter: NumberFormat): String =
    currencyFormatter.format(this ?: BigDecimal.ZERO)

/**
 * Formats this amount for a render-when-present line item (Storage), rendering `$0.00` for a
 * free line and returning `null` only when the amount is `null`.
 */
fun BigDecimal?.toPresentMoneyText(currencyFormatter: NumberFormat): String? =
    this?.let { currencyFormatter.format(it) }

/**
 * Formats this amount as a negative money string for the Discount line item (e.g. "-$5.00"),
 * returning `null` when the amount is `null` or non-positive so the row is omitted when there is
 * no discount.
 */
fun BigDecimal?.toDiscountMoneyText(currencyFormatter: NumberFormat): String? =
    this
        ?.takeIf { it.signum() > 0 }
        ?.let { "\u2212${currencyFormatter.format(it)}" }
