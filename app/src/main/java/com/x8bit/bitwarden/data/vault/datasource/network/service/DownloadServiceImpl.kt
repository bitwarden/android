package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import com.x8bit.bitwarden.data.vault.datasource.network.api.DownloadApi
import okhttp3.ResponseBody

/**
 * Default implementation of [DownloadService].
 */
class DownloadServiceImpl(
    private val downloadApi: DownloadApi,
) : DownloadService {
    override suspend fun getDataStream(
        url: String,
    ): Result<ResponseBody> =
        downloadApi
            .getDataStream(url = url)
            .toResult()
}
