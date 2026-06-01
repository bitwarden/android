package com.bitwarden.network.api

import com.bitwarden.network.model.NetworkResult
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Defines raw calls under the /devices API that do not require authentication.
 */
internal interface UnauthenticatedDevicesApi {
    @GET("/devices/knowndevice")
    suspend fun getIsKnownDevice(
        @Header(value = "X-Request-Email") emailAddress: String,
        @Header(value = "X-Device-Identifier") deviceId: String,
    ): NetworkResult<Boolean>
}
