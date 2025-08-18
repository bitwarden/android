package com.bitwarden.network.api

import androidx.annotation.Keep
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.TrustedDeviceKeysRequestJson
import com.bitwarden.network.model.TrustedDeviceKeysResponseJson
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /devices API that require authentication.
 */
@Keep
internal interface AuthenticatedDevicesApi {
    @PUT("/devices/{appId}/keys")
    suspend fun updateTrustedDeviceKeys(
        @Path(value = "appId") appId: String,
        @Body request: TrustedDeviceKeysRequestJson,
    ): NetworkResult<TrustedDeviceKeysResponseJson>
}
