package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request object for creating a Stripe checkout session for Premium upgrade.
 *
 * @property platform The platform identifier (e.g., "android" or "ios").
 */
@Serializable
data class CheckoutSessionRequestJson(
    @SerialName("platform")
    val platform: String,
)
