package com.x8bit.bitwarden.data.vault.datasource.disk

import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to vault data.
 */
interface VaultDiskSource {

    /**
     * Retrieves all ciphers from the data source for a given [userId].
     */
    fun getCiphers(userId: String): Flow<List<SyncResponseJson.Cipher>>

    /**
     * Replaces all [vault] data for a given [userId] with the new `vault`.
     */
    suspend fun replaceVaultData(userId: String, vault: SyncResponseJson)

    /**
     * Deletes all stored vault data from the data source for a given [userId].
     */
    suspend fun deleteVaultData(userId: String)
}
