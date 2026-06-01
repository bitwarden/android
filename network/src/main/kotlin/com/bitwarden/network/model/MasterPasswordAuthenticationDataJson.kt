package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the data used to authenticate with the master password.
 */
@Serializable
data class MasterPasswordAuthenticationDataJson(
    @SerialName("Kdf")
    val kdf: KdfJson,

    @SerialName("MasterPasswordAuthenticationHash")
    val masterPasswordAuthenticationHash: String,

    @SerialName("Salt")
    val salt: String,
)
