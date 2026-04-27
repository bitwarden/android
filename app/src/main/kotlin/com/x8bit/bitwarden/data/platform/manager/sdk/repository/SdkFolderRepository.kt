package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.sdk.FolderRepository
import com.bitwarden.vault.Folder
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkFolderResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import timber.log.Timber

/**
 * A user-scoped implementation of a Bitwarden SDK [FolderRepository].
 */
class SdkFolderRepository(
    private val userId: String,
    private val vaultDiskSource: VaultDiskSource,
) : FolderRepository {
    override suspend fun get(id: String): Folder? =
        vaultDiskSource
            .getFolder(userId = userId, folderId = id)
            ?.toEncryptedSdkFolder()

    override suspend fun list(): List<Folder> =
        vaultDiskSource
            .getFolders(userId = userId)
            .map { it.toEncryptedSdkFolder() }

    override suspend fun set(id: String, value: Folder) {
        if (id != value.id) {
            Timber.e("SDK Folder 'set' operation: ID's do not match")
            return
        }
        vaultDiskSource.saveFolder(
            userId = userId,
            folder = value.toEncryptedNetworkFolderResponse(),
        )
    }

    override suspend fun setBulk(values: Map<String, Folder>) {
        val validEntries = values.filter { (id, cipher) ->
            if (id != cipher.id) {
                Timber.e("SDK Folder 'setBulk' operation: ID's do not match for '$id'")
                false
            } else {
                true
            }
        }
        if (validEntries.isEmpty()) return
        vaultDiskSource.saveFolders(
            userId = userId,
            folders = validEntries.values.map { it.toEncryptedNetworkFolderResponse() },
        )
    }

    override suspend fun remove(id: String) {
        vaultDiskSource.deleteFolder(userId = userId, folderId = id)
    }

    override suspend fun removeBulk(keys: List<String>) {
        if (keys.isEmpty()) return
        vaultDiskSource.deleteSelectedFolders(userId = userId, folderIds = keys)
    }

    override suspend fun removeAll() {
        vaultDiskSource.deleteAllFolders(userId = userId)
    }

    override suspend fun has(id: String): Boolean = this.get(id = id) != null
}
