package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when creating a Premium checkout session.
 *
 * @property checkoutSessionUrl The Stripe checkout URL for Premium upgrade.
 */
@Serializable
data class CheckoutSessionResponseJson(
    @SerialName("checkoutSessionUrl")
    val checkoutSessionUrl: String,
)
