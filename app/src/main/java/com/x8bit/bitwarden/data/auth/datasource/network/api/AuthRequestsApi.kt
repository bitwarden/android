package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import retrofit2.http.GET

/**
 * Defines raw calls under the /auth-requests API.
 */
interface AuthRequestsApi {

    /**
     * Gets a list of auth requests for this device.
     */
    @GET("/auth-requests")
    suspend fun getAuthRequests(): Result<AuthRequestsResponseJson>
}
