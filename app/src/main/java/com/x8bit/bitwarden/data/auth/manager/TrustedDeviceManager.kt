package com.x8bit.bitwarden.data.auth.manager

/**
 * Manager used to establish trust with this device.
 */
interface TrustedDeviceManager {
    /**
     * Establishes trust with this device if necessary.
     */
    suspend fun trustThisDeviceIfNecessary(userId: String): Result<Boolean>
}
