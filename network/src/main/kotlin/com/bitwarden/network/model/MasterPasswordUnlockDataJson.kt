package com.bitwarden.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Represents the data used to unlock with the master password.
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class MasterPasswordUnlockDataJson(
    @SerialName("salt")
    val salt: String,

    @SerialName("kdf")
    val kdf: KdfJson,

    // TODO: PM-26397 this was done due to naming inconsistency server side,
    //  should be cleaned up when server side is updated
    @SerialName("masterKeyWrappedUserKey")
    @JsonNames("masterKeyEncryptedUserKey")
    val masterKeyWrappedUserKey: String,
)
