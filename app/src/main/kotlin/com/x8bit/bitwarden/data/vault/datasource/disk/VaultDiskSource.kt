package com.x8bit.bitwarden.data.vault.datasource.disk

import com.bitwarden.network.model.SyncResponseJson
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
     * Retrieves all ciphers from the data source for a given [userId] as a [Flow].
     */
    fun getCiphersFlow(userId: String): Flow<List<SyncResponseJson.Cipher>>

    /**
     * Retrieves all ciphers from the data source for a given [userId].
     */
    suspend fun getCiphers(userId: String): List<SyncResponseJson.Cipher>

    /**
     * Checks if the user has any personal ciphers (ciphers not belonging to an organization).
     *
     * This is an optimized query that checks only the indexed organizationId column
     * without loading full cipher JSON data. Intended for vault migration state checks.
     *
     * @return Flow that emits true if user has personal ciphers, false otherwise
     */
    fun hasPersonalCiphersFlow(userId: String): Flow<Boolean>

    /**
     * Retrieves all ciphers with the given [cipherIds] from the data source for a given [userId].
     */
    suspend fun getSelectedCiphers(
        userId: String,
        cipherIds: List<String>,
    ): List<SyncResponseJson.Cipher>

    /**
     * Retrieves all ciphers from the data source for a given [userId] that contain TOTP codes.
     */
    suspend fun getTotpCiphers(userId: String): List<SyncResponseJson.Cipher>

    /**
     * Retrieves a cipher from the data source for a given [userId] and [cipherId].
     */
    suspend fun getCipher(userId: String, cipherId: String): SyncResponseJson.Cipher?

    /**
     * Deletes a cipher from the data source for the given [userId] and [cipherId].
     */
    suspend fun deleteCipher(userId: String, cipherId: String)

    /**
     * Saves multiple ciphers to the data source for the given [userId].
     */
    suspend fun saveCiphers(userId: String, ciphers: List<SyncResponseJson.Cipher>)

    /**
     * Deletes ciphers with the given [cipherIds] from the data source for the given [userId].
     */
    suspend fun deleteSelectedCiphers(userId: String, cipherIds: List<String>)

    /**
     * Deletes all ciphers from the data source for the given [userId].
     */
    suspend fun deleteAllCiphers(userId: String)

    /**
     * Saves a collection to the data source for the given [userId].
     */
    suspend fun saveCollection(userId: String, collection: SyncResponseJson.Collection)

    /**
     * Retrieves all collections from the data source for a given [userId].
     */
    fun getCollectionsFlow(userId: String): Flow<List<SyncResponseJson.Collection>>

    /**
     * Retrieves all domains from the data source for a given [userId].
     */
    fun getDomainsFlow(userId: String): Flow<SyncResponseJson.Domains?>

    /**
     * Deletes a folder from the data source for the given [userId] and [folderId].
     */
    suspend fun deleteFolder(userId: String, folderId: String)

    /**
     * Deletes folders with the given [folderIds] from the data source for the given [userId].
     */
    suspend fun deleteSelectedFolders(userId: String, folderIds: List<String>)

    /**
     * Deletes all folders from the data source for the given [userId].
     */
    suspend fun deleteAllFolders(userId: String)

    /**
     * Saves a folder to the data source for the given [userId].
     */
    suspend fun saveFolder(userId: String, folder: SyncResponseJson.Folder)

    /**
     * Saves multiple folders to the data source for the given [userId].
     */
    suspend fun saveFolders(userId: String, folders: List<SyncResponseJson.Folder>)

    /**
     * Retrieves a folder from the data source for a given [userId] and [folderId].
     */
    suspend fun getFolder(userId: String, folderId: String): SyncResponseJson.Folder?

    /**
     * Retrieves all folders from the data source for a given [userId].
     */
    suspend fun getFolders(userId: String): List<SyncResponseJson.Folder>

    /**
     * Retrieves all folders from the data source for a given [userId].
     */
    fun getFoldersFlow(userId: String): Flow<List<SyncResponseJson.Folder>>

    /**
     * Saves a Send to the data source for the given [userId].
     */
    suspend fun saveSend(userId: String, send: SyncResponseJson.Send)

    /**
     * Deletes a Send from the data source for the given [userId] and [sendId].
     */
    suspend fun deleteSend(userId: String, sendId: String)

    /**
     * Retrieves all sends from the data source for a given [userId].
     */
    fun getSendsFlow(userId: String): Flow<List<SyncResponseJson.Send>>

    /**
     * Replaces all [vault] data for a given [userId] with the new `vault`.
     *
     * This will always cause the [getCiphersFlow], [getCollectionsFlow], and [getFoldersFlow]
     * functions to re-emit even if the underlying data has not changed.
     */
    suspend fun replaceVaultData(userId: String, vault: SyncResponseJson)

    /**
     * Trigger re-emissions from the [getCiphersFlow], [getCollectionsFlow], [getFoldersFlow],
     * and [getSendsFlow] functions.
     */
    suspend fun resyncVaultData(userId: String)

    /**
     * Deletes all stored vault data from the data source for a given [userId].
     */
    suspend fun deleteVaultData(userId: String)
}
