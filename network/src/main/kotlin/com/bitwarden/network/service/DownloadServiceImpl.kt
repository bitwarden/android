package com.bitwarden.network.service

import com.bitwarden.network.api.DownloadApi
import com.bitwarden.network.util.toResult
import okhttp3.ResponseBody

/**
 * Default implementation of [DownloadService].
 */
internal class DownloadServiceImpl(
    private val downloadApi: DownloadApi,
) : DownloadService {
    override suspend fun getDataStream(
        url: String,
    ): Result<ResponseBody> =
        downloadApi
            .getDataStream(url = url)
            .toResult()
}
