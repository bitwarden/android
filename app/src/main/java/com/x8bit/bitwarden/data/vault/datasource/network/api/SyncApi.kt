package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import retrofit2.http.GET

/**
 * This interface defines the API service for fetching vault data.
 */
interface SyncApi {
    /**
     * Retrieves the vault data from the server.
     *
     * @return A [SyncResponseJson] containing the vault response model.
     */
    @GET("sync")
    suspend fun sync(): NetworkResult<SyncResponseJson>

    @GET("/accounts/revision-date")
    suspend fun getAccountRevisionDateMillis(): NetworkResult<Long>
}
