package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the data used to unlock with the master password.
 */
@Serializable
data class MasterPasswordUnlockDataJson(
    @SerialName("Salt")
    val salt: String,

    @SerialName("Kdf")
    val kdf: KdfJson,

    @SerialName("MasterKeyWrappedUserKey")
    val masterKeyWrappedUserKey: String,
)
