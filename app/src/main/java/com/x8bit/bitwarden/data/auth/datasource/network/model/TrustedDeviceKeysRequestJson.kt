package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The request body for trusting a device.
 */
@Serializable
data class TrustedDeviceKeysRequestJson(
    @SerialName("EncryptedUserKey") val encryptedUserKey: String,
    @SerialName("EncryptedPublicKey") val encryptedDevicePublicKey: String,
    @SerialName("EncryptedPrivateKey") val encryptedDevicePrivateKey: String,
)
