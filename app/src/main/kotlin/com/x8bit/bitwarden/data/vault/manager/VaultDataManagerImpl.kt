package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.core.data.repository.util.map
import com.bitwarden.core.data.repository.util.mapNullable
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.util.isActive
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The default implementation of the [VaultDataManager].
 */
internal class VaultDataManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val cipherManager: CipherManager,
    private val totpCodeManager: TotpCodeManager,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSyncManager: VaultSyncManager,
    dispatcherManager: DispatcherManager,
) : VaultDataManager {
    private val ioScope = CoroutineScope(context = dispatcherManager.io)
    private val unconfinedScope = CoroutineScope(context = dispatcherManager.unconfined)
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override var vaultFilterType: VaultFilterType = VaultFilterType.AllVaults

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>> {
        val userId = activeUserId ?: return MutableStateFlow(
            DataState.Error(error = IllegalStateException("No active user"), null),
        )
        return vaultSyncManager
            .vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData
                        .decryptCipherListResult
                        .successes
                        .filter {
                            it.type is CipherListViewType.Login &&
                                !it.login?.totp.isNullOrBlank() &&
                                it.isActive
                        }
                        .toFilteredList(vaultFilterType = vaultFilterType)
                }
            }
            .flatMapLatest { cipherDataState ->
                val cipherList = cipherDataState.data ?: emptyList()
                totpCodeManager
                    .getTotpCodesForCipherListViewsStateFlow(
                        userId = userId,
                        cipherListViews = cipherList,
                    )
                    .map { verificationCodeDataStates ->
                        combineDataStates(
                            dataState1 = verificationCodeDataStates,
                            dataState2 = cipherDataState,
                        ) { verificationCodeItems, _ ->
                            // Just return the verification items; we are only combining the
                            // DataStates to know the overall state.
                            verificationCodeItems
                        }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DataState.Loading,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodeFlow(cipherId: String): StateFlow<DataState<VerificationCodeItem?>> {
        val userId = activeUserId ?: return MutableStateFlow(
            DataState.Error(error = IllegalStateException("No active user"), null),
        )
        return this.getVaultListItemStateFlow(cipherId)
            .flatMapLatest { cipherDataState ->
                cipherDataState
                    .data
                    ?.let {
                        totpCodeManager
                            .getTotpCodeStateFlow(userId = userId, cipherListView = it)
                            .map { totpCodeDataState ->
                                combineDataStates(
                                    dataState1 = totpCodeDataState,
                                    dataState2 = cipherDataState,
                                ) { _, _ ->
                                    // We are only combining the DataStates to know the overall
                                    // state, we map it to the appropriate value below.
                                }
                                    .mapNullable { totpCodeDataState.data }
                            }
                    }
                    ?: flowOf(DataState.Loaded(data = null))
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DataState.Loading,
            )
    }

    override fun deleteVaultData(userId: String) {
        ioScope.launch {
            vaultDiskSource.deleteVaultData(userId = userId)
        }
    }

    override fun getVaultItemStateFlow(
        itemId: String,
    ): StateFlow<DataState<CipherView?>> =
        vaultSyncManager
            .vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    val getCipherResult = vaultData
                        .decryptCipherListResult
                        .successes
                        .find { it.id == itemId }
                        .let { cipherManager.getCipher(cipherId = itemId) }
                    when (getCipherResult) {
                        is GetCipherResult.Success -> getCipherResult.cipherView
                        else -> null
                    }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )

    override fun getVaultListItemStateFlow(
        itemId: String,
    ): StateFlow<DataState<CipherListView?>> =
        vaultSyncManager
            .vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData.decryptCipherListResult.successes.find { it.id == itemId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )

    override fun getVaultFolderStateFlow(
        folderId: String,
    ): StateFlow<DataState<FolderView?>> =
        vaultSyncManager
            .vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData.folderViewList.find { it.id == folderId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )

    override fun getSendStateFlow(
        sendId: String,
    ): StateFlow<DataState<SendView?>> =
        vaultSyncManager
            .sendDataStateFlow
            .map { dataState ->
                dataState.map { sendData ->
                    sendData.sendViewList.find { it.id == sendId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )
}
