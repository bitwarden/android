package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when retrieving the premium plan pricing.
 *
 * @property name The display name of the plan.
 * @property legacyYear The legacy year identifier, if applicable.
 * @property isAvailable Whether the plan is currently available for purchase.
 * @property seat The seat pricing information.
 * @property storage The storage pricing information.
 */
@Serializable
data class PremiumPlanResponseJson(
    @SerialName("name")
    val name: String,

    @SerialName("legacyYear")
    val legacyYear: Int?,

    @SerialName("available")
    val isAvailable: Boolean,

    @SerialName("seat")
    val seat: PurchasableJson,

    @SerialName("storage")
    val storage: PurchasableJson,
) {

    /**
     * Purchasable item pricing details within a premium plan.
     *
     * @property stripePriceId The Stripe price identifier.
     * @property price The annual price.
     * @property provided The number of units provided by default.
     */
    @Serializable
    data class PurchasableJson(
        @SerialName("stripePriceId")
        val stripePriceId: String,

        @SerialName("price")
        val price: Double,

        @SerialName("provided")
        val provided: Int,
    )
}
