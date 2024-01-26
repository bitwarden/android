package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson

/**
 * Provides an API for creating a new authentication request.
 */
interface NewAuthRequestService {
    /**
     * Informs the server of a new auth request in order to notify approving devices.
     */
    suspend fun createAuthRequest(
        email: String,
        publicKey: String,
        deviceId: String,
        accessCode: String,
        fingerprint: String,
    ): Result<AuthRequestsResponseJson.AuthRequest>
}
