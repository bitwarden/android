package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body needed to PUT a GCM [pushToken] to Bitwarden's server.
 */
@Serializable
data class PushTokenRequest(
    @SerialName("pushToken") val pushToken: String,
)
