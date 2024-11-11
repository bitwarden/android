package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Url

/**
 * Defines raw calls to the Azure API without any authentication applied.
 */
interface AzureApi {
    /**
     * Attempts to upload an encrypted file to Azure.
     */
    @PUT
    @Headers("x-ms-blob-type: BlockBlob")
    suspend fun uploadAzureBlob(
        @Url url: String,
        @Header("x-ms-date") date: String,
        @Header("x-ms-version") version: String?,
        @Body body: RequestBody,
    ): NetworkResult<Unit>
}
