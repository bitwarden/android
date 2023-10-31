package com.x8bit.bitwarden.data.vault.repository

import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Default implementation of [VaultRepository].
 */
class VaultRepositoryImpl constructor(
    private val syncService: SyncService,
    private val vaultSdkSource: VaultSdkSource,
    dispatcher: CoroutineDispatcher,
) : VaultRepository {

    private val scope = CoroutineScope(dispatcher)

    private var syncJob: Job = Job().apply { complete() }

    override suspend fun sync() {
        if (!syncJob.isCompleted) return
        syncJob = scope.launch {
            syncService
                .sync()
                .fold(
                    onSuccess = { syncResponse ->
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
}
