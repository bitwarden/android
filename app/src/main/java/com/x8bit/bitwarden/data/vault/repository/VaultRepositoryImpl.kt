package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUpdatedUserStateJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.isNoConnectionError
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.data.platform.repository.util.map
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndLoggedIn
import com.x8bit.bitwarden.data.platform.repository.util.updateToPendingOrLoading
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 1000L

/**
 * Default implementation of [VaultRepository].
 */
@Suppress("TooManyFunctions")
class VaultRepositoryImpl(
    private val syncService: SyncService,
    private val ciphersService: CiphersService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    dispatcherManager: DispatcherManager,
) : VaultRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private var syncJob: Job = Job().apply { complete() }

    private var willSyncAfterUnlock = false

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableVaultStateStateFlow =
        MutableStateFlow(VaultState(unlockedVaultUserIds = emptySet()))

    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)

    private val mutableCiphersStateFlow =
        MutableStateFlow<DataState<List<CipherView>>>(DataState.Loading)

    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<List<FolderView>>>(DataState.Loading)

    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)

    override val vaultDataStateFlow: StateFlow<DataState<VaultData>> =
        combine(
            ciphersStateFlow,
            foldersStateFlow,
            collectionsStateFlow,
        ) { ciphersDataState, foldersDataState, collectionsDataState ->
            combineDataStates(
                ciphersDataState,
                foldersDataState,
                collectionsDataState,
            ) { ciphersData, foldersData, collectionsData ->
                VaultData(
                    cipherViewList = ciphersData,
                    folderViewList = foldersData,
                    collectionViewList = collectionsData,
                )
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    override val ciphersStateFlow: StateFlow<DataState<List<CipherView>>>
        get() = mutableCiphersStateFlow.asStateFlow()

    override val foldersStateFlow: StateFlow<DataState<List<FolderView>>>
        get() = mutableFoldersStateFlow.asStateFlow()

    override val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>
        get() = mutableCollectionsStateFlow.asStateFlow()

    override val vaultStateFlow: StateFlow<VaultState>
        get() = mutableVaultStateStateFlow.asStateFlow()

    override val sendDataStateFlow: StateFlow<DataState<SendData>>
        get() = mutableSendDataStateFlow.asStateFlow()

    init {
        // Setup ciphers MutableStateFlow
        mutableCiphersStateFlow
            .observeWhenSubscribedAndLoggedIn(authDiskSource.userStateFlow) { activeUserId ->
                observeVaultDiskCiphers(activeUserId)
            }
            .launchIn(unconfinedScope)
        // Setup folders MutableStateFlow
        mutableFoldersStateFlow
            .observeWhenSubscribedAndLoggedIn(authDiskSource.userStateFlow) { activeUserId ->
                observeVaultDiskFolders(activeUserId)
            }
            .launchIn(unconfinedScope)
        // Setup collections MutableStateFlow
        mutableCollectionsStateFlow
            .observeWhenSubscribedAndLoggedIn(authDiskSource.userStateFlow) { activeUserId ->
                observeVaultDiskCollections(activeUserId)
            }
            .launchIn(unconfinedScope)
    }

    override fun clearUnlockedData() {
        mutableCiphersStateFlow.update { DataState.Loading }
        mutableFoldersStateFlow.update { DataState.Loading }
        mutableCollectionsStateFlow.update { DataState.Loading }
        mutableSendDataStateFlow.update { DataState.Loading }
    }

    override fun deleteVaultData(userId: String) {
        ioScope.launch {
            vaultDiskSource.deleteVaultData(userId)
        }
    }

    override fun sync() {
        if (!syncJob.isCompleted || willSyncAfterUnlock) return
        val userId = activeUserId ?: return
        mutableCiphersStateFlow.updateToPendingOrLoading()
        mutableFoldersStateFlow.updateToPendingOrLoading()
        mutableCollectionsStateFlow.updateToPendingOrLoading()
        mutableSendDataStateFlow.updateToPendingOrLoading()
        syncJob = ioScope.launch {
            syncService
                .sync()
                .fold(
                    onSuccess = { syncResponse ->
                        // Update user information with additional information from sync response
                        authDiskSource.userState = authDiskSource
                            .userState
                            ?.toUpdatedUserStateJson(
                                syncResponse = syncResponse,
                            )

                        unlockVaultForOrganizationsIfNecessary(syncResponse = syncResponse)
                        storeKeys(syncResponse = syncResponse)
                        vaultDiskSource.replaceVaultData(userId = userId, vault = syncResponse)
                        decryptSendsAndUpdateSendDataState(sendList = syncResponse.sends)
                    },
                    onFailure = { throwable ->
                        mutableCiphersStateFlow.update { currentState ->
                            throwable.toNetworkOrErrorState(
                                data = currentState.data,
                            )
                        }
                        mutableFoldersStateFlow.update { currentState ->
                            throwable.toNetworkOrErrorState(
                                data = currentState.data,
                            )
                        }
                        mutableCollectionsStateFlow.update { currentState ->
                            throwable.toNetworkOrErrorState(
                                data = currentState.data,
                            )
                        }
                        mutableSendDataStateFlow.update { currentState ->
                            throwable.toNetworkOrErrorState(
                                data = currentState.data,
                            )
                        }
                    },
                )
        }
    }

    override fun getVaultItemStateFlow(itemId: String): StateFlow<DataState<CipherView?>> =
        vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData
                        .cipherViewList
                        .find { it.id == itemId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )

    override fun getVaultFolderStateFlow(folderId: String): StateFlow<DataState<FolderView?>> =
        vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData
                        .folderViewList
                        .find { it.id == folderId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = DataState.Loading,
            )

    override fun lockVaultForCurrentUser() {
        authDiskSource.userState?.activeUserId?.let {
            lockVaultIfNecessary(it)
        }
    }

    override fun lockVaultIfNecessary(userId: String) {
        setVaultToLocked(userId = userId)
    }

    @Suppress("ReturnCount")
    override suspend fun unlockVaultAndSyncForCurrentUser(
        masterPassword: String,
    ): VaultUnlockResult {
        val userState = authDiskSource.userState
            ?: return VaultUnlockResult.InvalidStateError
        val userKey = authDiskSource.getUserKey(userId = userState.activeUserId)
            ?: return VaultUnlockResult.InvalidStateError
        val privateKey = authDiskSource.getPrivateKey(userId = userState.activeUserId)
            ?: return VaultUnlockResult.InvalidStateError
        val organizationKeys = authDiskSource
            .getOrganizationKeys(userId = userState.activeUserId)
        return unlockVault(
            userId = userState.activeUserId,
            masterPassword = masterPassword,
            email = userState.activeAccount.profile.email,
            kdf = userState.activeAccount.profile.toSdkParams(),
            userKey = userKey,
            privateKey = privateKey,
            organizationKeys = organizationKeys,
        )
            .also {
                if (it is VaultUnlockResult.Success) {
                    sync()
                }
            }
    }

    override suspend fun unlockVault(
        userId: String,
        masterPassword: String,
        email: String,
        kdf: Kdf,
        userKey: String,
        privateKey: String,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult =
        flow {
            willSyncAfterUnlock = true
            emit(
                vaultSdkSource
                    .initializeCrypto(
                        request = InitUserCryptoRequest(
                            kdfParams = kdf,
                            email = email,
                            privateKey = privateKey,
                            method = InitUserCryptoMethod.Password(
                                password = masterPassword,
                                userKey = userKey,
                            ),
                        ),
                    )
                    .flatMap { result ->
                        // Initialize the SDK for organizations if necessary
                        if (organizationKeys != null &&
                            result is InitializeCryptoResult.Success
                        ) {
                            vaultSdkSource.initializeOrganizationCrypto(
                                request = InitOrgCryptoRequest(
                                    organizationKeys = organizationKeys,
                                ),
                            )
                        } else {
                            result.asSuccess()
                        }
                    }
                    .fold(
                        onFailure = { VaultUnlockResult.GenericError },
                        onSuccess = { initializeCryptoResult ->
                            initializeCryptoResult
                                .toVaultUnlockResult()
                                .also {
                                    if (it is VaultUnlockResult.Success) {
                                        setVaultToUnlocked(userId = userId)
                                    }
                                }
                        },
                    ),
            )
        }
            .onCompletion { willSyncAfterUnlock = false }
            .first()

    override suspend fun createCipher(cipherView: CipherView): CreateCipherResult =
        vaultSdkSource
            .encryptCipher(cipherView = cipherView)
            .flatMap { cipher ->
                ciphersService
                    .createCipher(
                        body = cipher.toEncryptedNetworkCipher(),
                    )
            }
            .fold(
                onFailure = {
                    CreateCipherResult.Error
                },
                onSuccess = {
                    sync()
                    CreateCipherResult.Success
                },
            )

    override suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult =
        vaultSdkSource
            .encryptCipher(cipherView = cipherView)
            .flatMap { cipher ->
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = cipher.toEncryptedNetworkCipher(),
                )
            }
            .fold(
                onFailure = { UpdateCipherResult.Error },
                onSuccess = {
                    sync()
                    UpdateCipherResult.Success
                },
            )

    // TODO: This is temporary. Eventually this needs to be based on the presence of various
    //  user keys but this will likely require SDK updates to support this (BIT-1190).
    private fun setVaultToUnlocked(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds + userId,
            )
        }
    }

    // TODO: This is temporary. Eventually this needs to be based on the presence of various
    //  user keys but this will likely require SDK updates to support this (BIT-1190).
    private fun setVaultToLocked(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds - userId,
            )
        }
    }

    private fun storeKeys(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val userId = profile.id
        val userKey = profile.key
        val privateKey = profile.privateKey
        authDiskSource.apply {
            storeUserKey(
                userId = userId,
                userKey = userKey,
            )
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
            storeOrganizationKeys(
                userId = profile.id,
                organizationKeys = profile.organizations
                    .orEmpty()
                    .filter { it.key != null }
                    .associate { it.id to requireNotNull(it.key) },
            )
        }
    }

    private suspend fun unlockVaultForOrganizationsIfNecessary(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val organizationKeys = profile.organizations
            .orEmpty()
            .filter { it.key != null }
            .associate { it.id to requireNotNull(it.key) }
            .takeUnless { it.isEmpty() }
            ?: return

        // There shouldn't be issues when unlocking directly from the syncResponse so we can ignore
        // the return type here.
        vaultSdkSource
            .initializeOrganizationCrypto(
                request = InitOrgCryptoRequest(
                    organizationKeys = organizationKeys,
                ),
            )
    }

    private suspend fun decryptSendsAndUpdateSendDataState(sendList: List<SyncResponseJson.Send>?) {
        val newState = vaultSdkSource
            .decryptSendList(
                sendList = sendList
                    .orEmpty()
                    .toEncryptedSdkSendList(),
            )
            .fold(
                onSuccess = { DataState.Loaded(data = SendData(sendViewList = it)) },
                onFailure = { DataState.Error(error = it) },
            )
        mutableSendDataStateFlow.update { newState }
    }

    private fun observeVaultDiskCiphers(
        userId: String,
    ): Flow<DataState<List<CipherView>>> =
        vaultDiskSource
            .getCiphers(userId = userId)
            .onStart { mutableCiphersStateFlow.value = DataState.Loading }
            .map {
                vaultSdkSource
                    .decryptCipherList(cipherList = it.toEncryptedSdkCipherList())
                    .fold(
                        onSuccess = { ciphers -> DataState.Loaded(ciphers) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .onEach { mutableCiphersStateFlow.value = it }

    private fun observeVaultDiskFolders(
        userId: String,
    ): Flow<DataState<List<FolderView>>> =
        vaultDiskSource
            .getFolders(userId = userId)
            .onStart { mutableFoldersStateFlow.value = DataState.Loading }
            .map {
                vaultSdkSource
                    .decryptFolderList(folderList = it.toEncryptedSdkFolderList())
                    .fold(
                        onSuccess = { folders -> DataState.Loaded(folders) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .onEach { mutableFoldersStateFlow.value = it }

    private fun observeVaultDiskCollections(
        userId: String,
    ): Flow<DataState<List<CollectionView>>> =
        vaultDiskSource
            .getCollections(userId = userId)
            .onStart { mutableCollectionsStateFlow.value = DataState.Loading }
            .map {
                vaultSdkSource
                    .decryptCollectionList(collectionList = it.toEncryptedSdkCollectionList())
                    .fold(
                        onSuccess = { collections -> DataState.Loaded(collections) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .onEach { mutableCollectionsStateFlow.value = it }
}

private fun <T> Throwable.toNetworkOrErrorState(data: T?): DataState<T> =
    if (isNoConnectionError()) {
        DataState.NoNetwork(data = data)
    } else {
        DataState.Error(
            error = this,
            data = data,
        )
    }
