package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the synchronization of the user's vault data with the remote server.
 * This interface provides a way to trigger a sync process, which updates the local
 * database with the latest changes from the server.
 */
interface VaultSyncManager {
    /**
     * Flow that represents the current vault data.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val vaultDataStateFlow: StateFlow<DataState<VaultData>>

    /**
     * Flow that represents all ciphers for the active user, including references to ciphers that
     * cannot be decrypted.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val decryptCipherListResultStateFlow: StateFlow<DataState<DecryptCipherListResult>>

    /**
     * Flow that represents all collections for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>

    /**
     * Flow that represents all domains for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val domainsStateFlow: StateFlow<DataState<DomainsData>>

    /**
     * Flow that represents all folders for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val foldersStateFlow: StateFlow<DataState<List<FolderView>>>

    /**
     * Flow that represents the current send data.
     */
    val sendDataStateFlow: StateFlow<DataState<SendData>>

    /**
     * Sync the vault data for the current user.
     *
     * Unlike [syncIfNecessary], this will always perform the requested sync and should only be
     * utilized in cases where the user specifically requested the action.
     */
    fun sync(forced: Boolean = false)

    /**
     * Checks if conditions have been met to perform a sync request and, if so, syncs the vault
     * data for the current user.
     */
    fun syncIfNecessary()

    /**
     * Syncs the vault data for the current user. This is an explicit request to sync and will
     * return the result of the sync as a [SyncVaultDataResult].
     */
    suspend fun syncForResult(forced: Boolean = false): SyncVaultDataResult
}
