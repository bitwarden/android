package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to unlock with the master password.
 */
@Serializable
data class MasterPasswordUnlockDataJsonRequest(
    @SerialName("Kdf")
    val kdf: KdfJsonRequest,

    @SerialName("MasterKeyWrappedUserKey")
    val masterKeyWrappedUserKey: String,

    @SerialName("Salt")
    val salt: String,
)
