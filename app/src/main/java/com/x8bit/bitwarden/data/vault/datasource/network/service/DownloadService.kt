package com.x8bit.bitwarden.data.vault.datasource.network.service

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
