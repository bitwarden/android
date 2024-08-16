package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to create the key connector keys for an account.
 */
@Serializable
data class KeyConnectorKeyRequestJson(
    @SerialName("key") val userKey: String,
    @SerialName("keys") val keys: Keys,
    @SerialName("kdf") val kdfType: KdfTypeJson,
    @SerialName("kdfIterations") val kdfIterations: Int?,
    @SerialName("kdfMemory") val kdfMemory: Int?,
    @SerialName("kdfParallelism") val kdfParallelism: Int?,
    @SerialName("orgIdentifier") val organizationIdentifier: String,
) {
    /**
     * A keys object containing public and private keys.
     *
     * @param publicKey the public key (encrypted).
     * @param encryptedPrivateKey the private key (encrypted).
     */
    @Serializable
    data class Keys(
        @SerialName("publicKey")
        val publicKey: String,

        @SerialName("encryptedPrivateKey")
        val encryptedPrivateKey: String,
    )
}
