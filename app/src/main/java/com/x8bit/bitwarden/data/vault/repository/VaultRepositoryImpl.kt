package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.InitCryptoRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.network.util.isNoConnectionError
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val vaultDataMutableStateFlow =
        MutableStateFlow<DataState<VaultData>>(DataState.Loading)

    override val vaultDataStateFlow: StateFlow<DataState<VaultData>>
        get() = vaultDataMutableStateFlow.asStateFlow()

    override fun clearVaultData() {
        vaultDataMutableStateFlow.update { DataState.Loading }
    }

    override fun sync() {
        if (!syncJob.isCompleted) return
        vaultDataMutableStateFlow.value.data?.let { data ->
            vaultDataMutableStateFlow.update {
                DataState.Pending(data = data)
            }
        }
        syncJob = scope.launch {
            syncService
                .sync()
                .fold(
                    onSuccess = { syncResponse ->
                        storeUserKeyAndPrivateKey(
                            userKey = syncResponse.profile?.key,
                            privateKey = syncResponse.profile?.privateKey,
                        )
                        decryptSyncResponseAndUpdateVaultDataState(
                            syncResponse = syncResponse,
                        )
                    },
                    onFailure = { throwable ->
                        vaultDataMutableStateFlow.update {
                            if (throwable.isNoConnectionError()) {
                                DataState.NoNetwork(
                                    data = it.data,
                                )
                            } else {
                                DataState.Error(
                                    error = throwable,
                                    data = it.data,
                                )
                            }
                        }
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

    private suspend fun decryptSyncResponseAndUpdateVaultDataState(syncResponse: SyncResponseJson) {
        val newState = vaultSdkSource
            .decryptCipherList(
                cipherList = (syncResponse.ciphers ?: emptyList())
                    .toEncryptedSdkCipherList(),
            )
            .flatMap { decryptedCipherList ->
                vaultSdkSource
                    .decryptFolderList(
                        folderList = (syncResponse.folders ?: emptyList())
                            .toEncryptedSdkFolderList(),
                    )
                    .map { decryptedFolderList ->
                        decryptedCipherList to decryptedFolderList
                    }
            }
            .fold(
                onSuccess = { (decryptedCipherList, decryptedFolderList) ->
                    DataState.Loaded(
                        data = VaultData(
                            cipherListViewList = decryptedCipherList,
                            folderViewList = decryptedFolderList,
                        ),
                    )
                },
                onFailure = { DataState.Error(error = it) },
            )
        vaultDataMutableStateFlow.update { newState }
    }
}
