package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the response body used to retrieve the master key from the cloud.
 */
@Serializable
data class KeyConnectorMasterKeyResponseJson(
    @SerialName("key") val masterKey: String,
)
