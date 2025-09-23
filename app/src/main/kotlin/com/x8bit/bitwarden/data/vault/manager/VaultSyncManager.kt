package com.x8bit.bitwarden.data.vault.manager

import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult

/**
 * Manages the synchronization of the user's vault data with the remote server.
 * This interface provides a way to trigger a sync process, which updates the local
 * database with the latest changes from the server.
 */
interface VaultSyncManager {
    /**
     * Initiates a synchronization process for the user's vault data.
     *
     * This function fetches the latest data from the remote server and updates the local
     * vault cache. It can be a standard sync or a "forced" sync, which typically
     * bypasses local cache checks and fetches everything anew.
     *
     * @param userId The unique identifier of the user whose vault is to be synchronized.
     * @param forced If true, performs a full, forced synchronization, ignoring any recent sync
     * timestamps. If false, performs a standard incremental sync.
     *
     * @return A [SyncVaultDataResult] indicating the outcome of the synchronization, such as
     * success or failure with details.
     */
    suspend fun sync(
        userId: String,
        forced: Boolean,
    ): SyncVaultDataResult
}
