package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to authenticate with the master password.
 */
@Serializable
data class MasterPasswordAuthenticationDataJsonRequest(
    @SerialName("Kdf")
    val kdf: KdfJsonRequest,

    @SerialName("MasterPasswordAuthenticationHash")
    val masterPasswordAuthenticationHash: String,

    @SerialName("Salt")
    val salt: String,
)
