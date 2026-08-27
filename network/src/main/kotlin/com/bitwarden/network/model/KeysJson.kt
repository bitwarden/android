package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A keys object containing public and private keys.
 *
 * @param publicKey the public key (encrypted).
 * @param encryptedPrivateKey the private key (encrypted).
 */
@Serializable
data class KeysJson(
    @SerialName("publicKey")
    val publicKey: String,

    @SerialName("encryptedPrivateKey")
    val encryptedPrivateKey: String,
)
