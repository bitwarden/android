package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to create the keys for an account.
 */
@Serializable
data class CreateAccountKeysRequest(
    @SerialName("PublicKey") val publicKey: String,
    @SerialName("EncryptedPrivateKey") val encryptedPrivateKey: String,
)
