package com.x8bit.bitwarden.data.auth.datasource.network.api

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_KEY_AUTHORIZATION
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Defines raw calls specific for key connectors that use custom urls.
 */
@Keep
interface UnauthenticatedKeyConnectorApi {
    @POST
    suspend fun storeMasterKeyToKeyConnector(
        @Url url: String,
        @Header(HEADER_KEY_AUTHORIZATION) bearerToken: String,
        @Body body: KeyConnectorMasterKeyRequestJson,
    ): NetworkResult<Unit>

    @GET
    suspend fun getMasterKeyFromKeyConnector(
        @Url url: String,
        @Header(HEADER_KEY_AUTHORIZATION) bearerToken: String,
    ): NetworkResult<KeyConnectorMasterKeyResponseJson>
}
