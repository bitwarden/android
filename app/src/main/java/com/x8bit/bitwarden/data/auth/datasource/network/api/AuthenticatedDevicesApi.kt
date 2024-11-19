package com.x8bit.bitwarden.data.auth.datasource.network.api

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /devices API that require authentication.
 */
@Keep
interface AuthenticatedDevicesApi {
    @PUT("/devices/{appId}/keys")
    suspend fun updateTrustedDeviceKeys(
        @Path(value = "appId") appId: String,
        @Body request: TrustedDeviceKeysRequestJson,
    ): NetworkResult<TrustedDeviceKeysResponseJson>
}
