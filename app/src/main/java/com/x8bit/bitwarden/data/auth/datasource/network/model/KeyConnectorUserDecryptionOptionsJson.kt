package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decryption options related to a user's key connector.
 *
 * @property keyConnectorUrl URL to the user's key connector.
 */
@Serializable
data class KeyConnectorUserDecryptionOptionsJson(
    @SerialName("KeyConnectorUrl")
    val keyConnectorUrl: String,
)
