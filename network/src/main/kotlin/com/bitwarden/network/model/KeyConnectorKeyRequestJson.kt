package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to create the key connector keys for an account.
 */
@Serializable
data class KeyConnectorKeyRequestJson(
    @SerialName("key") val userKey: String,
    @SerialName("keys") val keys: KeysJson,
    @SerialName("kdf") val kdfType: KdfTypeJson,
    @SerialName("kdfIterations") val kdfIterations: Int?,
    @SerialName("kdfMemory") val kdfMemory: Int?,
    @SerialName("kdfParallelism") val kdfParallelism: Int?,
    @SerialName("orgIdentifier") val organizationIdentifier: String,
)
