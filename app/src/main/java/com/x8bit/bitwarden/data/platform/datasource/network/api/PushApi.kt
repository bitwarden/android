package com.x8bit.bitwarden.data.platform.datasource.network.api

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines API calls for push tokens.
 */
interface PushApi {
    @PUT("/devices/identifier/{appId}/token")
    suspend fun putDeviceToken(
        @Path("appId") appId: String,
        @Body body: PushTokenRequest,
    ): NetworkResult<Unit>
}
