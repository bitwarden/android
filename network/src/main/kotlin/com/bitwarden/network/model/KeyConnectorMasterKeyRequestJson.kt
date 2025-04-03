package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to store the master key in the cloud.
 */
@Serializable
data class KeyConnectorMasterKeyRequestJson(
    @SerialName("Key") val masterKey: String,
)
