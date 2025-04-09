package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.util.toResult
import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi

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
