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
}
