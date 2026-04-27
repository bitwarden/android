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
 * @property cart The cart details of the subscription.
 * @property storage The storage usage details, if available.
 * @property cancelAt The date the subscription is scheduled to cancel, if applicable.
 * @property canceled The date the subscription was canceled, if applicable.
 * @property nextCharge The date of the next charge, if applicable.
 * @property suspension The date the subscription was suspended, if applicable.
 * @property gracePeriod The grace period in days, if applicable.
 */
@Serializable
data class BitwardenSubscriptionResponseJson(
    @SerialName("status")
    val status: SubscriptionStatusJson,

    @SerialName("cart")
    val cart: CartJson,

    @SerialName("storage")
    val storage: StorageJson?,

    @Contextual
    @SerialName("cancelAt")
    val cancelAt: Instant?,

    @Contextual
    @SerialName("canceled")
    val canceled: Instant?,

    @Contextual
    @SerialName("nextCharge")
    val nextCharge: Instant?,

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

    @SerialName("paused")
    PAUSED,
}

/**
 * Represents the cart details of a subscription.
 *
 * @property passwordManager The password manager cart items.
 * @property secretsManager The secrets manager cart items, if applicable.
 * @property cadence The billing cadence of the subscription.
 * @property discount The discount applied to the cart, if applicable.
 * @property estimatedTax The estimated tax amount.
 */
@Serializable
data class CartJson(
    @SerialName("passwordManager")
    val passwordManager: PasswordManagerCartItemsJson,

    @SerialName("secretsManager")
    val secretsManager: SecretsManagerCartItemsJson?,

    @SerialName("cadence")
    val cadence: CadenceTypeJson,

    @SerialName("discount")
    val discount: BitwardenDiscountJson?,

    @Contextual
    @SerialName("estimatedTax")
    val estimatedTax: BigDecimal,
)

/**
 * Represents the password manager cart items within a subscription.
 *
 * @property seats The seat pricing details.
 * @property additionalStorage The additional storage pricing details, if applicable.
 */
@Serializable
data class PasswordManagerCartItemsJson(
    @SerialName("seats")
    val seats: CartItemJson,

    @SerialName("additionalStorage")
    val additionalStorage: CartItemJson?,
)

/**
 * Represents the secrets manager cart items within a subscription.
 *
 * @property seats The seat pricing details.
 * @property additionalServiceAccounts The additional service accounts pricing details,
 * if applicable.
 */
@Serializable
data class SecretsManagerCartItemsJson(
    @SerialName("seats")
    val seats: CartItemJson,

    @SerialName("additionalServiceAccounts")
    val additionalServiceAccounts: CartItemJson?,
)

/**
 * Represents a single cart item within a subscription.
 *
 * @property translationKey The translation key for display purposes.
 * @property quantity The quantity of this item.
 * @property cost The cost of this item.
 * @property discount The discount applied to this item, if applicable.
 */
@Serializable
data class CartItemJson(
    @SerialName("translationKey")
    val translationKey: String,

    @SerialName("quantity")
    val quantity: Long,

    @Contextual
    @SerialName("cost")
    val cost: BigDecimal,

    @SerialName("discount")
    val discount: BitwardenDiscountJson?,
)

/**
 * Represents a discount applied to a subscription or cart item.
 *
 * @property type The type of discount.
 * @property value The discount value.
 */
@Serializable
data class BitwardenDiscountJson(
    @SerialName("type")
    val type: DiscountTypeJson,

    @Contextual
    @SerialName("value")
    val value: BigDecimal,
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
