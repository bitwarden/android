package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /devices API that require authentication.
 */
interface AuthenticatedDevicesApi {
    @PUT("/devices/{appId}/keys")
    suspend fun updateTrustedDeviceKeys(
        @Path(value = "appId") appId: String,
        @Body request: TrustedDeviceKeysRequestJson,
    ): Result<TrustedDeviceKeysResponseJson>
}
