package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Decryption options related to a user's key connector.
 *
 * @property keyConnectorUrl URL to the user's key connector.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class KeyConnectorUserDecryptionOptionsJson(
    @SerialName("keyConnectorUrl")
    @JsonNames("KeyConnectorUrl")
    val keyConnectorUrl: String,
)
