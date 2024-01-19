package com.x8bit.bitwarden.data.auth.datasource.network.service

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
}
