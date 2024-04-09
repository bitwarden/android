package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.crypto.TrustDeviceResponse

/**
 * Manager used to establish trust with this device.
 */
interface TrustedDeviceManager {
    /**
     * Establishes trust with this device if necessary.
     */
    suspend fun trustThisDeviceIfNecessary(userId: String): Result<Boolean>

    /**
     * Establishes trust with this device based on the provided [TrustDeviceResponse].
     */
    suspend fun trustThisDevice(
        userId: String,
        trustDeviceResponse: TrustDeviceResponse,
    ): Result<Unit>
}
