package com.x8bit.bitwarden.data.vault.datasource.disk

import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to vault data.
 */
@Suppress("TooManyFunctions")
interface VaultDiskSource {

    /**
     * Saves a cipher to the data source for the given [userId].
     */
    suspend fun saveCipher(userId: String, cipher: SyncResponseJson.Cipher)

    /**
     * Retrieves all ciphers from the data source for a given [userId].
     */
    fun getCiphers(userId: String): Flow<List<SyncResponseJson.Cipher>>

    /**
     * Deletes a cipher from the data source for the given [userId] and [cipherId].
     */
    suspend fun deleteCipher(userId: String, cipherId: String)

    /**
     * Saves a collection to the data source for the given [userId].
     */
    suspend fun saveCollection(userId: String, collection: SyncResponseJson.Collection)

    /**
     * Retrieves all collections from the data source for a given [userId].
     */
    fun getCollections(userId: String): Flow<List<SyncResponseJson.Collection>>

    /**
     * Retrieves all domains from the data source for a given [userId].
     */
    fun getDomains(userId: String): Flow<SyncResponseJson.Domains?>

    /**
     * Deletes a folder from the data source for the given [userId] and [folderId].
     */
    suspend fun deleteFolder(userId: String, folderId: String)

    /**
     * Saves a folder to the data source for the given [userId].
     */
    suspend fun saveFolder(userId: String, folder: SyncResponseJson.Folder)

    /**
     * Retrieves all folders from the data source for a given [userId].
     */
    fun getFolders(userId: String): Flow<List<SyncResponseJson.Folder>>

    /**
     * Saves a send to the data source for the given [userId].
     */
    suspend fun saveSend(userId: String, send: SyncResponseJson.Send)

    /**
     * Deletes a send from the data source for the given [userId] and [sendId].
     */
    suspend fun deleteSend(userId: String, sendId: String)

    /**
     * Retrieves all sends from the data source for a given [userId].
     */
    fun getSends(userId: String): Flow<List<SyncResponseJson.Send>>

    /**
     * Replaces all [vault] data for a given [userId] with the new `vault`.
     *
     * This will always cause the [getCiphers], [getCollections], and [getFolders] functions to
     * re-emit even if the underlying data has not changed.
     */
    suspend fun replaceVaultData(userId: String, vault: SyncResponseJson)

    /**
     * Trigger re-emissions from the [getCiphers], [getCollections], [getFolders], and [getSends]
     * functions.
     */
    suspend fun resyncVaultData(userId: String)

    /**
     * Deletes all stored vault data from the data source for a given [userId].
     */
    suspend fun deleteVaultData(userId: String)
}
