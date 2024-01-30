package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestUpdateRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines authenticated raw calls under the /auth-requests API.
 */
interface AuthenticatedAuthRequestsApi {

    /**
     * Updates an authentication request.
     */
    @PUT("/auth-requests/{id}")
    suspend fun updateAuthRequest(
        @Path("id") userId: String,
        @Body body: AuthRequestUpdateRequestJson,
    ): Result<AuthRequestsResponseJson.AuthRequest>

    /**
     * Gets a list of auth requests for this device.
     */
    @GET("/auth-requests")
    suspend fun getAuthRequests(): Result<AuthRequestsResponseJson>
}
