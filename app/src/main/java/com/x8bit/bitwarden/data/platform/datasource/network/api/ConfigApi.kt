package com.x8bit.bitwarden.data.platform.datasource.network.api

import com.bitwarden.network.model.NetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import retrofit2.http.GET

/**
 * This interface defines the API service for fetching configuration data.
 */
interface ConfigApi {

    @GET("config")
    suspend fun getConfig(): NetworkResult<ConfigResponseJson>
}
