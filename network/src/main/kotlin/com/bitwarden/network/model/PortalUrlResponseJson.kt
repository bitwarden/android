package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when requesting a Stripe customer portal session.
 *
 * @property url The Stripe customer portal URL.
 */
@Serializable
data class PortalUrlResponseJson(
    @SerialName("url")
    val url: String,
)
