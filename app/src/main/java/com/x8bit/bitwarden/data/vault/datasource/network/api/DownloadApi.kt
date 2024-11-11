package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Defines endpoints to retrieve content from arbitrary URLs.
 */
interface DownloadApi {
    /**
     * Streams data from a [url].
     */
    @GET
    @Streaming
    suspend fun getDataStream(
        @Url url: String,
    ): NetworkResult<ResponseBody>
}
