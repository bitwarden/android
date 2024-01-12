package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.Kdf
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUpdatedUserStateJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.isNoConnectionError
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.data.platform.repository.util.map
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndLoggedIn
import com.x8bit.bitwarden.data.platform.repository.util.updateToPendingOrLoading
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkSend
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSend
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
@Suppress("TooManyFunctions", "LongParameterList")
class VaultRepositoryImpl(
    private val syncService: SyncService,
    private val ciphersService: CiphersService,
    private val sendsService: SendsService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val vaultLockManager: VaultLockManager,
    dispatcherManager: DispatcherManager,
) : VaultRepository,
    VaultLockManager by vaultLockManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private var syncJob: Job = Job().apply { complete() }

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableTotpCodeResultFlow = bufferedMutableSharedFlow<TotpCodeResult>()

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
            sendDataStateFlow,
        ) { ciphersDataState, foldersDataState, collectionsDataState, sendsDataState ->
            combineDataStates(
                ciphersDataState,
                foldersDataState,
                collectionsDataState,
                sendsDataState,
            ) { ciphersData, foldersData, collectionsData, sendsData ->
                VaultData(
                    cipherViewList = ciphersData,
                    folderViewList = foldersData,
                    collectionViewList = collectionsData,
                    sendViewList = sendsData.sendViewList,
                )
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    override val totpCodeFlow: Flow<TotpCodeResult>
        get() = mutableTotpCodeResultFlow.asSharedFlow()

    override val ciphersStateFlow: StateFlow<DataState<List<CipherView>>>
        get() = mutableCiphersStateFlow.asStateFlow()

    override val foldersStateFlow: StateFlow<DataState<List<FolderView>>>
        get() = mutableFoldersStateFlow.asStateFlow()

    override val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>
        get() = mutableCollectionsStateFlow.asStateFlow()

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
        // Setup sends MutableStateFlow
        mutableSendDataStateFlow
            .observeWhenSubscribedAndLoggedIn(authDiskSource.userStateFlow) { activeUserId ->
                observeVaultDiskSends(activeUserId)
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
        val userId = activeUserId ?: return
        if (!syncJob.isCompleted || isVaultUnlocking(userId)) return
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
                        storeProfileData(syncResponse = syncResponse)
                        vaultDiskSource.replaceVaultData(userId = userId, vault = syncResponse)
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

    override fun getSendStateFlow(sendId: String): StateFlow<DataState<SendView?>> =
        sendDataStateFlow
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

    override fun emitTotpCodeResult(totpCodeResult: TotpCodeResult) {
        mutableTotpCodeResultFlow.tryEmit(totpCodeResult)
    }

    @Suppress("ReturnCount")
    override suspend fun unlockVaultAndSyncForCurrentUser(
        masterPassword: String,
    ): VaultUnlockResult {
        val userId = activeUserId ?: return VaultUnlockResult.InvalidStateError
        val userKey = authDiskSource.getUserKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError
        return unlockVaultForUser(
            userId = userId,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
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
        unlockVault(
            userId = userId,
            email = email,
            kdf = kdf,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
            organizationKeys = organizationKeys,
        )

    override suspend fun createCipher(cipherView: CipherView): CreateCipherResult {
        val userId = requireNotNull(activeUserId)
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
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
                    vaultDiskSource.saveCipher(userId = userId, cipher = it)
                    CreateCipherResult.Success
                },
            )
    }

    override suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult {
        val userId = requireNotNull(activeUserId)
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { cipher ->
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = cipher.toEncryptedNetworkCipher(),
                )
            }
            .fold(
                onFailure = { UpdateCipherResult.Error(errorMessage = null) },
                onSuccess = { response ->
                    when (response) {
                        is UpdateCipherResponseJson.Invalid -> {
                            UpdateCipherResult.Error(errorMessage = response.message)
                        }

                        is UpdateCipherResponseJson.Success -> {
                            vaultDiskSource.saveCipher(userId = userId, cipher = response.cipher)
                            UpdateCipherResult.Success
                        }
                    }
                },
            )
    }

    override suspend fun createSend(sendView: SendView): CreateSendResult {
        val userId = requireNotNull(activeUserId)
        return vaultSdkSource
            .encryptSend(
                userId = userId,
                sendView = sendView,
            )
            .flatMap { send -> sendsService.createSend(body = send.toEncryptedNetworkSend()) }
            .onSuccess {
                // Save the send immediately, regardless of whether the decrypt succeeds
                vaultDiskSource.saveSend(userId = userId, send = it)
            }
            .flatMap {
                vaultSdkSource.decryptSend(
                    userId = userId,
                    send = it.toEncryptedSdkSend(),
                )
            }
            .fold(
                onFailure = { CreateSendResult.Error },
                onSuccess = { CreateSendResult.Success(it) },
            )
    }

    override suspend fun updateSend(
        sendId: String,
        sendView: SendView,
    ): UpdateSendResult {
        val userId = requireNotNull(activeUserId)
        return vaultSdkSource
            .encryptSend(
                userId = userId,
                sendView = sendView,
            )
            .flatMap { send ->
                sendsService.updateSend(
                    sendId = sendId,
                    body = send.toEncryptedNetworkSend(),
                )
            }
            .fold(
                onFailure = { UpdateSendResult.Error(errorMessage = null) },
                onSuccess = { response ->
                    when (response) {
                        is UpdateSendResponseJson.Invalid -> {
                            UpdateSendResult.Error(errorMessage = response.message)
                        }

                        is UpdateSendResponseJson.Success -> {
                            vaultDiskSource.saveSend(userId = userId, send = response.send)
                            vaultSdkSource
                                .decryptSend(
                                    userId = userId,
                                    send = response.send.toEncryptedSdkSend(),
                                )
                                .getOrNull()
                                ?.let { UpdateSendResult.Success(sendView = it) }
                                ?: UpdateSendResult.Error(errorMessage = null)
                        }
                    }
                },
            )
    }

    override suspend fun removePasswordSend(sendId: String): RemovePasswordSendResult {
        val userId = requireNotNull(activeUserId)
        return sendsService
            .removeSendPassword(sendId = sendId)
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is UpdateSendResponseJson.Invalid -> {
                            RemovePasswordSendResult.Error(errorMessage = response.message)
                        }

                        is UpdateSendResponseJson.Success -> {
                            vaultDiskSource.saveSend(userId = userId, send = response.send)
                            vaultSdkSource
                                .decryptSend(
                                    userId = userId,
                                    send = response.send.toEncryptedSdkSend(),
                                )
                                .getOrNull()
                                ?.let { RemovePasswordSendResult.Success(sendView = it) }
                                ?: RemovePasswordSendResult.Error(errorMessage = null)
                        }
                    }
                },
                onFailure = { RemovePasswordSendResult.Error(errorMessage = null) },
            )
    }

    private fun storeProfileData(
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
            storeOrganizations(
                userId = profile.id,
                organizations = syncResponse.profile.organizations,
            )
        }
    }

    @Suppress("ReturnCount")
    private suspend fun unlockVaultForUser(
        userId: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val account = authDiskSource.userState?.accounts?.get(userId)
            ?: return VaultUnlockResult.InvalidStateError
        val privateKey = authDiskSource.getPrivateKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError
        val organizationKeys = authDiskSource
            .getOrganizationKeys(userId = userId)
        return unlockVault(
            userId = userId,
            email = account.profile.email,
            kdf = account.profile.toSdkParams(),
            privateKey = privateKey,
            initUserCryptoMethod = initUserCryptoMethod,
            organizationKeys = organizationKeys,
        )
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
                userId = syncResponse.profile.id,
                request = InitOrgCryptoRequest(
                    organizationKeys = organizationKeys,
                ),
            )
    }

    private fun observeVaultDiskCiphers(
        userId: String,
    ): Flow<DataState<List<CipherView>>> =
        vaultDiskSource
            .getCiphers(userId = userId)
            .onStart {
                mutableCiphersStateFlow.value = DataState.Loading
            }
            .map {
                vaultSdkSource
                    .decryptCipherList(
                        userId = userId,
                        cipherList = it.toEncryptedSdkCipherList(),
                    )
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
                    .decryptFolderList(
                        userId = userId,
                        folderList = it.toEncryptedSdkFolderList(),
                    )
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
                    .decryptCollectionList(
                        userId = userId,
                        collectionList = it.toEncryptedSdkCollectionList(),
                    )
                    .fold(
                        onSuccess = { collections -> DataState.Loaded(collections) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .onEach { mutableCollectionsStateFlow.value = it }

    private fun observeVaultDiskSends(
        userId: String,
    ): Flow<DataState<SendData>> =
        vaultDiskSource
            .getSends(userId = userId)
            .onStart { mutableSendDataStateFlow.value = DataState.Loading }
            .map {
                vaultSdkSource
                    .decryptSendList(
                        userId = userId,
                        sendList = it.toEncryptedSdkSendList(),
                    )
                    .fold(
                        onSuccess = { sends -> DataState.Loaded(SendData(sends)) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .onEach { mutableSendDataStateFlow.value = it }
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
