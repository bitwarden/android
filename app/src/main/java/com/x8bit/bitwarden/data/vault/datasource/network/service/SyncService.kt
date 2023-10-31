package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

/**
 * Provides an API for querying sync endpoints.
 */
interface SyncService {
    /**
     * Make sync request to get vault items.
     */
    suspend fun sync(): Result<SyncResponseJson>
}
