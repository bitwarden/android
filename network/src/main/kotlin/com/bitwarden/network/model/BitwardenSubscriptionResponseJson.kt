package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Response object returned when retrieving the user's premium subscription details.
 *
 * @property status The current status of the subscription.
 * @property invoicePreview The invoice preview details of the subscription.
 * @property storage The storage usage details, if available.
 * @property cancelAt The date the subscription is scheduled to cancel, if applicable.
 * @property canceled The date the subscription was canceled, if applicable.
 * @property suspension The date the subscription was suspended, if applicable.
 * @property gracePeriod The grace period in days, if applicable.
 */
@Serializable
data class BitwardenSubscriptionResponseJson(
    @SerialName("status")
    val status: SubscriptionStatusJson,

    @SerialName("invoicePreview")
    val invoicePreview: InvoicePreviewJson,

    @SerialName("storage")
    val storage: StorageJson?,

    @Contextual
    @SerialName("cancelAt")
    val cancelAt: Instant?,

    @Contextual
    @SerialName("canceled")
    val canceled: Instant?,

    @Contextual
    @SerialName("suspension")
    val suspension: Instant?,

    @SerialName("gracePeriod")
    val gracePeriod: Int?,
)

/**
 * Represents the status of a subscription.
 */
@Serializable
enum class SubscriptionStatusJson {
    @SerialName("active")
    ACTIVE,

    @SerialName("canceled")
    CANCELED,

    @SerialName("past_due")
    PAST_DUE,

    @SerialName("incomplete")
    INCOMPLETE,

    @SerialName("incomplete_expired")
    INCOMPLETE_EXPIRED,

    @SerialName("unpaid")
    UNPAID,

    @SerialName("trialing")
    TRIALING,
}

/**
 * Represents the invoice preview details of a subscription.
 *
 * @property passwordManager The password manager invoice preview items.
 * @property cadence The billing cadence of the subscription.
 * @property discounts The discounts applied to the invoice, if applicable.
 * @property startingBalance The starting balance of the invoice, if applicable.
 * @property estimatedTax The estimated tax amount.
 * @property total The total amount of the invoice.
 * @property amountDue The amount due on the invoice.
 * @property nextPaymentAttempt The billing-period boundary of the subscription, if applicable.
 */
@Serializable
data class InvoicePreviewJson(
    @SerialName("passwordManager")
    val passwordManager: PasswordManagerInvoicePreviewJson,

    @SerialName("cadence")
    val cadence: CadenceTypeJson,

    @SerialName("discounts")
    val discounts: List<InvoicePreviewDiscountJson>?,

    @Contextual
    @SerialName("startingBalance")
    val startingBalance: BigDecimal?,

    @Contextual
    @SerialName("estimatedTax")
    val estimatedTax: BigDecimal,

    @Contextual
    @SerialName("total")
    val total: BigDecimal,

    @Contextual
    @SerialName("amountDue")
    val amountDue: BigDecimal,

    @Contextual
    @SerialName("nextPaymentAttempt")
    val nextPaymentAttempt: Instant?,
)

/**
 * Represents the password manager invoice preview items within a subscription.
 *
 * @property seats The seat pricing details, if applicable.
 * @property additionalStorage The additional storage pricing details, if applicable.
 */
@Serializable
data class PasswordManagerInvoicePreviewJson(
    @SerialName("seats")
    val seats: InvoicePreviewItemJson?,

    @SerialName("additionalStorage")
    val additionalStorage: InvoicePreviewItemJson?,
)

/**
 * Represents a single invoice preview item within a subscription.
 *
 * @property quantity The quantity of this item.
 * @property cost The cost of this item.
 * @property discounts The discounts applied to this item, if applicable.
 */
@Serializable
data class InvoicePreviewItemJson(
    @SerialName("quantity")
    val quantity: Long,

    @Contextual
    @SerialName("cost")
    val cost: BigDecimal,

    @SerialName("discounts")
    val discounts: List<InvoicePreviewDiscountJson>?,
)

/**
 * Represents a discount applied to an invoice preview or invoice preview item.
 *
 * @property type The type of discount.
 * @property value The discount value, either a whole-number percentage or dollars off.
 * @property amount The applied discount amount in dollars.
 * @property label The coupon name, if applicable.
 * @property durationInMonths The number of months the coupon repeats for, if applicable.
 */
@Serializable
data class InvoicePreviewDiscountJson(
    @SerialName("type")
    val type: DiscountTypeJson,

    @Contextual
    @SerialName("value")
    val value: BigDecimal,

    @Contextual
    @SerialName("amount")
    val amount: BigDecimal,

    @SerialName("label")
    val label: String?,

    @SerialName("durationInMonths")
    val durationInMonths: Int?,
)

/**
 * Represents the type of discount applied to a subscription.
 */
@Serializable
enum class DiscountTypeJson {
    @SerialName("amount-off")
    AMOUNT_OFF,

    @SerialName("percent-off")
    PERCENT_OFF,
}

/**
 * Represents the billing cadence of a subscription.
 */
@Serializable
enum class CadenceTypeJson {
    @SerialName("annually")
    ANNUALLY,

    @SerialName("monthly")
    MONTHLY,
}

/**
 * Represents storage usage details for a subscription.
 *
 * @property available The available storage in bytes.
 * @property used The used storage amount.
 * @property readableUsed A human-readable representation of the used storage.
 */
@Serializable
data class StorageJson(
    @SerialName("available")
    val available: Int,

    @SerialName("used")
    val used: Double,

    @SerialName("readableUsed")
    val readableUsed: String,
)
