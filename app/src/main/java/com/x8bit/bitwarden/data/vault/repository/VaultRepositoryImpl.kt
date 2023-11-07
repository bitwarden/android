package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.InitCryptoRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
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

    override fun sync() {
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

    override suspend fun unlockVaultAndSync(masterPassword: String): VaultUnlockResult {
        return initializeCrypto(masterPassword = masterPassword)
            .also { vaultUnlockedResult ->
                if (vaultUnlockedResult is VaultUnlockResult.Success) sync()
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

    @Suppress("ReturnCount")
    private suspend fun initializeCrypto(masterPassword: String): VaultUnlockResult {
        val userState = authDiskSource.userState
            ?: return VaultUnlockResult.InvalidStateError
        val userKey = authDiskSource.getUserKey(userId = userState.activeUserId)
            ?: return VaultUnlockResult.InvalidStateError
        val privateKey = authDiskSource.getPrivateKey(userId = userState.activeUserId)
            ?: return VaultUnlockResult.InvalidStateError
        return vaultSdkSource
            .initializeCrypto(
                request = InitCryptoRequest(
                    kdfParams = userState.activeAccount.profile.toSdkParams(),
                    email = userState.activeAccount.profile.email,
                    password = masterPassword,
                    userKey = userKey,
                    privateKey = privateKey,
                    // TODO use actual organization keys BIT-1091
                    organizationKeys = mapOf(),
                ),
            )
            .fold(
                onFailure = { VaultUnlockResult.GenericError },
                onSuccess = { it.toVaultUnlockResult() },
            )
    }
}
