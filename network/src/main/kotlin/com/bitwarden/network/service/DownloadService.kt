package com.bitwarden.network.service

import okhttp3.ResponseBody

/**
 * Provides an API for querying arbitrary endpoints.
 */
interface DownloadService {
    /**
     * Streams data from [url], returning a raw [ResponseBody].
     */
    suspend fun getDataStream(url: String): Result<ResponseBody>
}
