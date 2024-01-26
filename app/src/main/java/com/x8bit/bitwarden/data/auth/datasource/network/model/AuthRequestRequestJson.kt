package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for creating an auth request.
 */
@Serializable
data class AuthRequestRequestJson(
    @SerialName("email")
    val email: String,

    @SerialName("publicKey")
    val publicKey: String,

    @SerialName("deviceIdentifier")
    val deviceId: String,

    @SerialName("accessCode")
    val accessCode: String,

    @SerialName("type")
    val type: AuthRequestTypeJson,

    @SerialName("fingerprintPhrase")
    val fingerprint: String,
)
