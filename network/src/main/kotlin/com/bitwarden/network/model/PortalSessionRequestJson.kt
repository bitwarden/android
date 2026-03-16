package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request object for creating a Stripe customer portal session.
 *
 * @property returnUrl The URL to redirect the user to after visiting the portal.
 */
@Serializable
data class PortalSessionRequestJson(
    @SerialName("returnUrl")
    val returnUrl: String,
)
