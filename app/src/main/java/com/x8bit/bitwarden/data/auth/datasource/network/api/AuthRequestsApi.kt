package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Defines raw calls under the /auth-requests API.
 */
interface AuthRequestsApi {

    /**
     * Notifies the server of a new authentication request.
     */
    @POST("/auth-requests")
    suspend fun createAuthRequest(
        @Body body: AuthRequestRequestJson,
    ): Result<AuthRequestsResponseJson.AuthRequest>

    /**
     * Gets a list of auth requests for this device.
     */
    @GET("/auth-requests")
    suspend fun getAuthRequests(): Result<AuthRequestsResponseJson>
}
