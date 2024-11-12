package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Defines unauthenticated raw calls under the /auth-requests API.
 */
interface UnauthenticatedAuthRequestsApi {

    /**
     * Notifies the server of a new authentication request.
     */
    @POST("/auth-requests")
    suspend fun createAuthRequest(
        @Header("Device-Identifier") deviceIdentifier: String,
        @Body body: AuthRequestRequestJson,
    ): NetworkResult<AuthRequestsResponseJson.AuthRequest>

    /**
     * Queries for updates to a given auth request.
     */
    @GET("/auth-requests/{requestId}/response")
    suspend fun getAuthRequestUpdate(
        @Path("requestId") requestId: String,
        @Query("code") accessCode: String,
    ): NetworkResult<AuthRequestsResponseJson.AuthRequest>
}
