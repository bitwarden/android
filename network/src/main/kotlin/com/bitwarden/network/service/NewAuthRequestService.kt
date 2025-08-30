package com.bitwarden.network.service

import com.bitwarden.network.model.AuthRequestTypeJson
import com.bitwarden.network.model.AuthRequestsResponseJson

/**
 * Provides an API for creating a new authentication request.
 */
interface NewAuthRequestService {
    /**
     * Informs the server of a new auth request in order to notify approving devices.
     */
    @Suppress("LongParameterList")
    suspend fun createAuthRequest(
        email: String,
        publicKey: String,
        deviceId: String,
        accessCode: String,
        fingerprint: String,
        authRequestType: AuthRequestTypeJson,
    ): Result<AuthRequestsResponseJson.AuthRequest>

    /**
     * Queries for updates to a given auth request.
     */
    suspend fun getAuthRequestUpdate(
        requestId: String,
        accessCode: String,
        isSso: Boolean,
    ): Result<AuthRequestsResponseJson.AuthRequest>
}
