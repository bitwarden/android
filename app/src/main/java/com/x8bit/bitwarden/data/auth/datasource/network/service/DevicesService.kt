package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson

/**
 * Provides an API for interacting with the /devices endpoints.
 */
interface DevicesService {
    /**
     * Check whether this device is known (and thus whether Login with Device is available).
     */
    suspend fun getIsKnownDevice(
        emailAddress: String,
        deviceId: String,
    ): Result<Boolean>

    /**
     * Establishes trust with this device by storing the encrypted keys in the cloud.
     */
    suspend fun trustDevice(
        appId: String,
        encryptedUserKey: String,
        encryptedDevicePublicKey: String,
        encryptedDevicePrivateKey: String,
    ): Result<TrustedDeviceKeysResponseJson>
}
