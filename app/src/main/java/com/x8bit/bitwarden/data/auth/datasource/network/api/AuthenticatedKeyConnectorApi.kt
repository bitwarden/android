package com.x8bit.bitwarden.data.auth.datasource.network.api

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Defines raw calls specific for key connectors that use custom urls.
 */
@Keep
interface AuthenticatedKeyConnectorApi {
    @POST
    suspend fun storeMasterKeyToKeyConnector(
        @Url url: String,
        @Body body: KeyConnectorMasterKeyRequestJson,
    ): NetworkResult<Unit>
}
