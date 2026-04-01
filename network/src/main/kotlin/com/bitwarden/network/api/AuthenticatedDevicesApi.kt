package com.bitwarden.network.api

import androidx.annotation.Keep
import com.bitwarden.network.model.DeviceResponseJson
import com.bitwarden.network.model.DevicesResponseJson
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.TrustedDeviceKeysRequestJson
import com.bitwarden.network.model.TrustedDeviceKeysResponseJson
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines raw calls under the /devices API that require authentication.
 */
@Keep
internal interface AuthenticatedDevicesApi {
    @GET("/devices")
    suspend fun getDevices(): NetworkResult<DevicesResponseJson>

    @GET("/devices/identifier/{deviceIdentifier}")
    suspend fun getDeviceByIdentifier(
        @Path(value = "deviceIdentifier") deviceIdentifier: String,
    ): NetworkResult<DeviceResponseJson>

    @PUT("/devices/{appId}/keys")
    suspend fun updateTrustedDeviceKeys(
        @Path(value = "appId") appId: String,
        @Body request: TrustedDeviceKeysRequestJson,
    ): NetworkResult<TrustedDeviceKeysResponseJson>
}
