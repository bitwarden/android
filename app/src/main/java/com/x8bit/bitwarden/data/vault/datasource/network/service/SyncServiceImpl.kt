package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.api.SyncApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

class SyncServiceImpl constructor(
    private val syncApi: SyncApi,
) : SyncService {
    override suspend fun sync(): Result<SyncResponseJson> = syncApi.sync()
}
