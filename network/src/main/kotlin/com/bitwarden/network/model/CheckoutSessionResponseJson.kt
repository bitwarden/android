package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when creating a premium checkout session.
 *
 * @property checkoutSessionUrl The Stripe checkout URL for premium upgrade.
 */
@Serializable
data class CheckoutSessionResponseJson(
    @SerialName("checkoutSessionUrl")
    val checkoutSessionUrl: String,
)
