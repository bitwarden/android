package com.x8bit.bitwarden.data.vault.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Default implementation of [VaultRepository].
 */
class VaultRepositoryImpl constructor(
    private val syncService: SyncService,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    dispatcherManager: DispatcherManager,
) : VaultRepository {

    private val scope = CoroutineScope(dispatcherManager.io)

    private var syncJob: Job = Job().apply { complete() }

    override suspend fun sync() {
        if (!syncJob.isCompleted) return
        syncJob = scope.launch {
            syncService
                .sync()
                .fold(
                    onSuccess = { syncResponse ->
                        storeUserKeyAndPrivateKey(
                            userKey = syncResponse.profile?.key,
                            privateKey = syncResponse.profile?.privateKey,
                        )
                        // TODO transform into domain object consumable by VaultViewModel BIT-205.
                        // TODO initialize crypto in BIT-990
                        syncResponse.ciphers?.let { networkCiphers ->
                            vaultSdkSource.decryptCipherList(
                                cipherList = networkCiphers.toEncryptedSdkCipherList(),
                            )
                        }
                        syncResponse.folders?.let { networkFolders ->
                            vaultSdkSource.decryptFolderList(
                                folderList = networkFolders.toEncryptedSdkFolderList(),
                            )
                        }
                    },
                    onFailure = {
                        // TODO handle failure BIT-205.
                    },
                )
        }
    }

    private fun storeUserKeyAndPrivateKey(
        userKey: String?,
        privateKey: String?,
    ) {
        val userId = authDiskSource.userState?.activeUserId ?: return
        if (userKey == null || privateKey == null) return
        authDiskSource.apply {
            storeUserKey(
                userId = userId,
                userKey = userKey,
            )
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
        }
    }
}
