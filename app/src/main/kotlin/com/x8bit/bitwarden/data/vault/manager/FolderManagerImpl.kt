package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.network.model.UpdateFolderResponseJson
import com.bitwarden.network.service.FolderService
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The default implementation of the [FolderManager].
 */
class FolderManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val folderService: FolderService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    dispatcherManager: DispatcherManager,
    pushManager: PushManager,
) : FolderManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    init {
        pushManager
            .syncFolderDeleteFlow
            .onEach(::deleteFolder)
            .launchIn(unconfinedScope)

        pushManager
            .syncFolderUpsertFlow
            .onEach(::syncFolderIfNecessary)
            .launchIn(ioScope)
    }

    override suspend fun createFolder(folderView: FolderView): CreateFolderResult {
        val userId = activeUserId ?: return CreateFolderResult.Error(NoActiveUserException())
        return vaultSdkSource
            .encryptFolder(userId = userId, folder = folderView)
            .flatMap { folderService.createFolder(body = it.toEncryptedNetworkFolder()) }
            .onSuccess { vaultDiskSource.saveFolder(userId = userId, folder = it) }
            .flatMap {
                vaultSdkSource.decryptFolder(userId = userId, folder = it.toEncryptedSdkFolder())
            }
            .fold(
                onSuccess = { CreateFolderResult.Success(folderView = it) },
                onFailure = { CreateFolderResult.Error(error = it) },
            )
    }

    override suspend fun deleteFolder(folderId: String): DeleteFolderResult {
        val userId = activeUserId ?: return DeleteFolderResult.Error(NoActiveUserException())
        return folderService
            .deleteFolder(folderId = folderId)
            .onSuccess {
                clearFolderIdFromCiphers(userId = userId, folderId = folderId)
                vaultDiskSource.deleteFolder(userId = userId, folderId = folderId)
            }
            .fold(
                onSuccess = { DeleteFolderResult.Success },
                onFailure = { DeleteFolderResult.Error(error = it) },
            )
    }

    override suspend fun updateFolder(
        folderId: String,
        folderView: FolderView,
    ): UpdateFolderResult {
        val userId = activeUserId ?: return UpdateFolderResult.Error(
            errorMessage = null,
            error = NoActiveUserException(),
        )
        return vaultSdkSource
            .encryptFolder(userId = userId, folder = folderView)
            .flatMap { folder ->
                folderService.updateFolder(
                    folderId = folder.id.toString(),
                    body = folder.toEncryptedNetworkFolder(),
                )
            }
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is UpdateFolderResponseJson.Success -> {
                            vaultDiskSource.saveFolder(userId = userId, folder = response.folder)
                            vaultSdkSource
                                .decryptFolder(
                                    userId = userId,
                                    folder = response.folder.toEncryptedSdkFolder(),
                                )
                                .fold(
                                    onSuccess = { UpdateFolderResult.Success(it) },
                                    onFailure = {
                                        UpdateFolderResult.Error(errorMessage = null, error = it)
                                    },
                                )
                        }

                        is UpdateFolderResponseJson.Invalid -> {
                            UpdateFolderResult.Error(errorMessage = response.message, error = null)
                        }
                    }
                },
                onFailure = { UpdateFolderResult.Error(it.message, error = it) },
            )
    }

    private suspend fun clearFolderIdFromCiphers(userId: String, folderId: String) {
        vaultDiskSource.getCiphers(userId = userId).forEach {
            if (it.folderId == folderId) {
                vaultDiskSource.saveCipher(userId = userId, cipher = it.copy(folderId = null))
            }
        }
    }

    /**
     * Deletes the folder specified by [syncFolderDeleteData] from disk.
     */
    private suspend fun deleteFolder(syncFolderDeleteData: SyncFolderDeleteData) {
        clearFolderIdFromCiphers(
            folderId = syncFolderDeleteData.folderId,
            userId = syncFolderDeleteData.userId,
        )
        vaultDiskSource.deleteFolder(
            folderId = syncFolderDeleteData.folderId,
            userId = syncFolderDeleteData.userId,
        )
    }

    /**
     * Syncs an individual folder contained in [syncFolderUpsertData] to disk if certain criteria
     * are met.
     */
    private suspend fun syncFolderIfNecessary(syncFolderUpsertData: SyncFolderUpsertData) {
        val userId = activeUserId ?: return
        val folderId = syncFolderUpsertData.folderId
        val isUpdate = syncFolderUpsertData.isUpdate
        val revisionDate = syncFolderUpsertData.revisionDate
        val localFolder = vaultDiskSource
            .getFolders(userId = userId)
            .first()
            .find { it.id == folderId }
        val isValidCreate = !isUpdate && localFolder == null
        val isValidUpdate = isUpdate &&
            localFolder != null &&
            localFolder.revisionDate.toEpochSecond() < revisionDate.toEpochSecond()

        if (!isValidCreate && !isValidUpdate) return

        folderService
            .getFolder(folderId = folderId)
            .onSuccess { vaultDiskSource.saveFolder(userId = userId, folder = it) }
    }
}
