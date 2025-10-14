package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the request body used to update the user's kdf settings.
 */
@Serializable
data class UpdateKdfJsonRequest(
    @SerialName("authenticationData")
    val authenticationData: MasterPasswordAuthenticationDataJson,

    @SerialName("key")
    val key: String,

    @SerialName("masterPasswordHash")
    val masterPasswordHash: String,

    @SerialName("newMasterPasswordHash")
    val newMasterPasswordHash: String,

    @SerialName("unlockData")
    val unlockData: MasterPasswordUnlockDataJson,
)
