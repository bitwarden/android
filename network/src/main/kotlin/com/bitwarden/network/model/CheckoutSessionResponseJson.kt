package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when creating a premium checkout session.
 *
 * @property checkoutUrl The Stripe checkout URL for premium upgrade.
 */
@Serializable
data class CheckoutSessionResponseJson(
    @SerialName("checkoutUrl")
    val checkoutUrl: String,
)
