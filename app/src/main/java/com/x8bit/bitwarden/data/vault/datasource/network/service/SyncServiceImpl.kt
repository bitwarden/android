package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

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
