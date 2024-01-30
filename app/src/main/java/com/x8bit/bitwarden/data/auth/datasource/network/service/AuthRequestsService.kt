package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson

/**
 * Provides an API for interacting with login approval / authentication requests.
 */
interface AuthRequestsService {
    /**
     * Gets the list of auth requests for the current user.
     */
    suspend fun getAuthRequests(): Result<AuthRequestsResponseJson>

    /**
     * Retrieves an existing auth request to see if a device has approved it.
     */
    suspend fun getAuthRequest(
        requestId: String,
    ): Result<AuthRequestsResponseJson.AuthRequest>

    /**
     * Updates an approval request.
     */
    suspend fun updateAuthRequest(
        requestId: String,
        key: String,
        masterPasswordHash: String?,
        deviceId: String,
        isApproved: Boolean,
    ): Result<AuthRequestsResponseJson.AuthRequest>
}
