package com.bitwarden.authenticator.data.platform.datasource.network.api

import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import retrofit2.http.GET

/**
 * This interface defines the API service for fetching configuration data.
 */
interface ConfigApi {

    @GET("config")
    suspend fun getConfig(): Result<ConfigResponseJson>
}
