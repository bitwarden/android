package com.bitwarden.network.service

import com.bitwarden.network.api.SyncApi
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.util.toResult

class SyncServiceImpl(
    private val syncApi: SyncApi,
) : SyncService {
    override suspend fun sync(): Result<SyncResponseJson> = syncApi
        .sync()
        .toResult()

    override suspend fun getAccountRevisionDateMillis(): Result<Long> =
        syncApi
            .getAccountRevisionDateMillis()
            .toResult()
}
