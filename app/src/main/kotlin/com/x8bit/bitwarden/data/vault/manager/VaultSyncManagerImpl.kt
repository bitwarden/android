package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionView
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.core.data.repository.util.map
import com.bitwarden.core.data.repository.util.updateToPendingOrLoading
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.service.SyncService
import com.bitwarden.network.util.isNoConnectionError
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.util.toUpdatedUserStateJson
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.error.SecurityStampMismatchException
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndLoggedIn
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndUnlocked
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.util.sortAlphabetically
import com.x8bit.bitwarden.data.vault.repository.util.sortAlphabeticallyByTypeAndOrganization
import com.x8bit.bitwarden.data.vault.repository.util.toDomainsData
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.temporal.ChronoUnit

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 1000L
private const val SYNC_IF_NECESSARY_DELAY_MIN: Long = 30L

/**
 * Default implementation of [VaultSyncManager].
 */
@Suppress("LongParameterList", "TooManyFunctions")
class VaultSyncManagerImpl(
    private val syncService: SyncService,
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val userLogoutManager: UserLogoutManager,
    private val userStateManager: UserStateManager,
    private val vaultLockManager: VaultLockManager,
    private val clock: Clock,
    databaseSchemeManager: DatabaseSchemeManager,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
) : VaultSyncManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private var syncJob: Job = Job().apply { complete() }

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)

    private val mutableDecryptCipherListResultFlow =
        MutableStateFlow<DataState<DecryptCipherListResult>>(DataState.Loading)

    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<List<FolderView>>>(DataState.Loading)

    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)

    private val mutableDomainsStateFlow =
        MutableStateFlow<DataState<DomainsData>>(DataState.Loading)

    override val decryptCipherListResultStateFlow: StateFlow<DataState<DecryptCipherListResult>>
        get() = mutableDecryptCipherListResultFlow.asStateFlow()

    override val domainsStateFlow: StateFlow<DataState<DomainsData>>
        get() = mutableDomainsStateFlow.asStateFlow()

    override val foldersStateFlow: StateFlow<DataState<List<FolderView>>>
        get() = mutableFoldersStateFlow.asStateFlow()

    override val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>
        get() = mutableCollectionsStateFlow.asStateFlow()

    override val sendDataStateFlow: StateFlow<DataState<SendData>>
        get() = mutableSendDataStateFlow.asStateFlow()

    override val vaultDataStateFlow: StateFlow<DataState<VaultData>> =
        combine(
            decryptCipherListResultStateFlow,
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
                    decryptCipherListResult = ciphersData,
                    collectionViewList = collectionsData,
                    folderViewList = foldersData,
                    sendViewList = sendsData.sendViewList,
                )
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    init {
        // Cancel any ongoing sync request and clear the vault data in memory every time
        // the user switches or the vault is locked for the active user.
        merge(
            authDiskSource
                .userSwitchingChangesFlow
                .onEach {
                    // DomainState is not part of the locked data but should still be cleared
                    // when the user changes
                    mutableDomainsStateFlow.update { DataState.Loading }
                },
            vaultLockManager
                .vaultUnlockDataStateFlow
                .filter { vaultUnlockDataList ->
                    // Clear if the active user is not currently unlocking or unlocked
                    vaultUnlockDataList.none { it.userId == activeUserId }
                },
        )
            .onEach {
                syncJob.cancel()
                clearUnlockedData()
            }
            .launchIn(unconfinedScope)

        // Setup ciphers MutableStateFlow
        mutableDecryptCipherListResultFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultLockManager.vaultUnlockDataStateFlow,
            ) { activeUserId -> observeVaultDiskCiphersToCipherListView(userId = activeUserId) }
            .launchIn(unconfinedScope)

        // Setup domains MutableStateFlow
        mutableDomainsStateFlow
            .observeWhenSubscribedAndLoggedIn(
                userStateFlow = authDiskSource.userStateFlow,
            ) { activeUserId -> observeVaultDiskDomains(userId = activeUserId) }
            .launchIn(unconfinedScope)
        // Setup folders MutableStateFlow
        mutableFoldersStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultLockManager.vaultUnlockDataStateFlow,
            ) { activeUserId -> observeVaultDiskFolders(userId = activeUserId) }
            .launchIn(unconfinedScope)
        // Setup collections MutableStateFlow
        mutableCollectionsStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultLockManager.vaultUnlockDataStateFlow,
            ) { activeUserId -> observeVaultDiskCollections(userId = activeUserId) }
            .launchIn(unconfinedScope)
        // Setup sends MutableStateFlow
        mutableSendDataStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultLockManager.vaultUnlockDataStateFlow,
            ) { activeUserId -> observeVaultDiskSends(userId = activeUserId) }
            .launchIn(unconfinedScope)

        pushManager
            .fullSyncFlow
            .onEach { userId ->
                if (userId == activeUserId) {
                    sync(forced = false)
                } else {
                    settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
                }
            }
            .launchIn(unconfinedScope)

        databaseSchemeManager
            .databaseSchemeChangeFlow
            .onEach { sync(forced = true) }
            .launchIn(ioScope)
    }

    override fun sync(forced: Boolean) {
        val userId = activeUserId ?: return
        if (!syncJob.isCompleted) return
        mutableDecryptCipherListResultFlow.updateToPendingOrLoading()
        mutableDomainsStateFlow.updateToPendingOrLoading()
        mutableFoldersStateFlow.updateToPendingOrLoading()
        mutableCollectionsStateFlow.updateToPendingOrLoading()
        mutableSendDataStateFlow.updateToPendingOrLoading()
        syncJob = ioScope.launch { syncInternal(userId = userId, forced = forced) }
    }

    override fun syncIfNecessary() {
        val userId = activeUserId ?: return
        // Sync if we have never done so or the last time was at last 30 minutes ago.
        val shouldSync = settingsDiskSource
            .getLastSyncTime(userId = userId)
            ?.let {
                clock.instant().isAfter(it.plus(SYNC_IF_NECESSARY_DELAY_MIN, ChronoUnit.MINUTES))
            }
            ?: true
        if (shouldSync) {
            sync(forced = false)
        }
    }

    override suspend fun syncForResult(forced: Boolean): SyncVaultDataResult {
        val userId = activeUserId ?: return SyncVaultDataResult.Error(NoActiveUserException())
        syncJob = ioScope
            .async { syncInternal(userId = userId, forced = forced) }
            .also {
                return try {
                    it.await()
                } catch (e: CancellationException) {
                    SyncVaultDataResult.Error(throwable = e)
                }
            }
    }

    @Suppress("LongMethod")
    private suspend fun syncInternal(
        userId: String,
        forced: Boolean,
    ): SyncVaultDataResult {
        if (!forced) {
            // Skip this check if we are forcing the request.
            val lastSyncInstant = settingsDiskSource
                .getLastSyncTime(userId = userId)
                ?.toEpochMilli()
            lastSyncInstant?.let { lastSyncTimeMs ->
                // If the lasSyncState is null we just sync, no checks required.
                syncService.getAccountRevisionDateMillis().fold(
                    onSuccess = { serverRevisionDate ->
                        if (serverRevisionDate < lastSyncTimeMs) {
                            // We can skip the actual sync call if there is no new data or
                            // database scheme changes since the last sync.
                            settingsDiskSource.storeLastSyncTime(
                                userId = userId,
                                lastSyncTime = clock.instant(),
                            )
                            vaultDiskSource.resyncVaultData(userId = userId)
                            val itemsAvailable = vaultDiskSource
                                .getCiphersFlow(userId)
                                .firstOrNull()
                                ?.isNotEmpty() == true
                            return SyncVaultDataResult.Success(itemsAvailable = itemsAvailable)
                        }
                    },
                    onFailure = {
                        updateVaultStateFlowsToError(throwable = it)
                        return SyncVaultDataResult.Error(throwable = it)
                    },
                )
            }
        }

        return syncService.sync().fold(
            onSuccess = { syncResponse ->
                userStateManager.userStateTransaction {
                    val localSecurityStamp = authDiskSource.userState?.activeAccount?.profile?.stamp
                    val serverSecurityStamp = syncResponse.profile.securityStamp
                    // Log the user out if the stamps do not match
                    localSecurityStamp?.let {
                        if (serverSecurityStamp != localSecurityStamp) {
                            // Ensure UserLogoutManager is available
                            userLogoutManager.softLogout(
                                userId = userId,
                                reason = LogoutReason.SecurityStamp,
                            )
                            return@userStateTransaction SyncVaultDataResult.Error(
                                SecurityStampMismatchException(),
                            )
                        }
                    }

                    // Update user information with additional information from sync response
                    authDiskSource.userState = authDiskSource.userState?.toUpdatedUserStateJson(
                        syncResponse = syncResponse,
                    )

                    unlockVaultForOrganizationsIfNecessary(syncResponse = syncResponse)
                    storeProfileData(syncResponse = syncResponse)

                    // Treat absent network policies as known empty data to
                    // distinguish between unknown null data.
                    authDiskSource.storePolicies(
                        userId = userId,
                        policies = syncResponse.policies.orEmpty(),
                    )

                    settingsDiskSource.storeLastSyncTime(
                        userId = userId,
                        lastSyncTime = clock.instant(),
                    )
                    vaultDiskSource.replaceVaultData(userId = userId, vault = syncResponse)
                    val itemsAvailable = syncResponse.ciphers?.isNotEmpty() == true
                    SyncVaultDataResult.Success(itemsAvailable = itemsAvailable)
                }
            },
            onFailure = {
                updateVaultStateFlowsToError(throwable = it)
                SyncVaultDataResult.Error(throwable = it)
            },
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
        vaultSdkSource.initializeOrganizationCrypto(
            userId = syncResponse.profile.id,
            request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
        )
    }

    private fun storeProfileData(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val userId = profile.id
        authDiskSource.apply {
            storeUserKey(userId = userId, userKey = profile.key)
            storePrivateKey(userId = userId, privateKey = profile.privateKey)
            storeAccountKeys(userId = userId, accountKeys = profile.accountKeys)
            storeOrganizationKeys(
                userId = userId,
                organizationKeys = profile.organizations
                    .orEmpty()
                    .filter { it.key != null }
                    .associate { it.id to requireNotNull(it.key) },
            )
            storeShouldUseKeyConnector(
                userId = userId,
                shouldUseKeyConnector = profile.shouldUseKeyConnector,
            )
            storeOrganizations(userId = userId, organizations = profile.organizations)
        }
    }

    private fun observeVaultDiskCiphersToCipherListView(
        userId: String,
    ): Flow<DataState<DecryptCipherListResult>> =
        vaultDiskSource
            .getCiphersFlow(userId = userId)
            .onStart { mutableDecryptCipherListResultFlow.updateToPendingOrLoading() }
            .map {
                vaultLockManager.waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptCipherListWithFailures(
                        userId = userId,
                        cipherList = it.toEncryptedSdkCipherList(),
                    )
                    .fold(
                        onSuccess = { result ->
                            DataState.Loaded(
                                result.copy(successes = result.successes.sortAlphabetically()),
                            )
                        },
                        onFailure = { throwable -> DataState.Error(error = throwable) },
                    )
            }
            .map {
                it
                    .takeUnless { settingsDiskSource.getLastSyncTime(userId = userId) == null }
                    ?: DataState.Loading
            }
            .onEach { mutableDecryptCipherListResultFlow.value = it }

    private fun observeVaultDiskDomains(
        userId: String,
    ): Flow<DataState<DomainsData>> =
        vaultDiskSource
            .getDomains(userId = userId)
            .onStart { mutableDomainsStateFlow.updateToPendingOrLoading() }
            .map { DataState.Loaded(data = it.toDomainsData()) }
            .onEach { mutableDomainsStateFlow.value = it }

    private fun observeVaultDiskFolders(
        userId: String,
    ): Flow<DataState<List<FolderView>>> =
        vaultDiskSource
            .getFolders(userId = userId)
            .onStart { mutableFoldersStateFlow.updateToPendingOrLoading() }
            .map {
                vaultLockManager.waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptFolderList(userId = userId, folderList = it.toEncryptedSdkFolderList())
                    .fold(
                        onSuccess = { folders -> DataState.Loaded(folders.sortAlphabetically()) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .map { it.orLoadingIfNotSynced(userId = userId) }
            .onEach { mutableFoldersStateFlow.value = it }

    private fun observeVaultDiskCollections(
        userId: String,
    ): Flow<DataState<List<CollectionView>>> =
        vaultDiskSource
            .getCollections(userId = userId)
            .onStart { mutableCollectionsStateFlow.updateToPendingOrLoading() }
            .map {
                vaultLockManager.waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptCollectionList(
                        userId = userId,
                        collectionList = it.toEncryptedSdkCollectionList(),
                    )
                    .fold(
                        onSuccess = { collections ->
                            DataState.Loaded(
                                data = collections.sortAlphabeticallyByTypeAndOrganization(
                                    userOrganizations = authDiskSource
                                        .getOrganizations(userId = userId)
                                        .orEmpty(),
                                ),
                            )
                        },
                        onFailure = { throwable -> DataState.Error(error = throwable) },
                    )
            }
            .map { it.orLoadingIfNotSynced(userId = userId) }
            .onEach { mutableCollectionsStateFlow.value = it }

    private fun observeVaultDiskSends(
        userId: String,
    ): Flow<DataState<SendData>> =
        vaultDiskSource
            .getSends(userId = userId)
            .onStart { mutableSendDataStateFlow.updateToPendingOrLoading() }
            .map {
                vaultLockManager.waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptSendList(userId = userId, sendList = it.toEncryptedSdkSendList())
                    .fold(
                        onSuccess = { sends -> DataState.Loaded(sends.sortAlphabetically()) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .map { it.orLoadingIfNotSynced(userId = userId) }
            .map { dataState -> dataState.map { SendData(sendViewList = it) } }
            .onEach { mutableSendDataStateFlow.value = it }

    private fun clearUnlockedData() {
        mutableDecryptCipherListResultFlow.update { DataState.Loading }
        mutableFoldersStateFlow.update { DataState.Loading }
        mutableCollectionsStateFlow.update { DataState.Loading }
        mutableSendDataStateFlow.update { DataState.Loading }
    }

    private fun updateVaultStateFlowsToError(throwable: Throwable) {
        mutableDecryptCipherListResultFlow.update { currentState ->
            throwable.toNetworkOrErrorState(data = currentState.data)
        }
        mutableDomainsStateFlow.update { currentState ->
            throwable.toNetworkOrErrorState(data = currentState.data)
        }
        mutableFoldersStateFlow.update { currentState ->
            throwable.toNetworkOrErrorState(data = currentState.data)
        }
        mutableCollectionsStateFlow.update { currentState ->
            throwable.toNetworkOrErrorState(data = currentState.data)
        }
        mutableSendDataStateFlow.update { currentState ->
            throwable.toNetworkOrErrorState(data = currentState.data)
        }
    }

    /**
     * Returns the given [DataState] as-is, or [DataState.Loading] if vault data for the given
     * [userId] has not synced. This can be used to distinguish between empty data in the database
     * because we are in the process of syncing from legitimately having no vault data.
     */
    private fun <T> DataState<List<T>>.orLoadingIfNotSynced(
        userId: String,
    ): DataState<List<T>> =
        this
            .takeUnless { settingsDiskSource.getLastSyncTime(userId = userId) == null }
            ?: DataState.Loading
}

private fun <T> Throwable.toNetworkOrErrorState(
    data: T?,
): DataState<T> =
    if (isNoConnectionError()) {
        DataState.NoNetwork(data = data)
    } else {
        DataState.Error(error = this, data = data)
    }
