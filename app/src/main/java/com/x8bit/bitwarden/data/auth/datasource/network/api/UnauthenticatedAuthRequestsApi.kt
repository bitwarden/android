package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines unauthenticated raw calls under the /auth-requests API.
 */
interface UnauthenticatedAuthRequestsApi {

    /**
     * Notifies the server of a new authentication request.
     */
    @POST("/auth-requests")
    suspend fun createAuthRequest(
        @Body body: AuthRequestRequestJson,
    ): Result<AuthRequestsResponseJson.AuthRequest>
}
