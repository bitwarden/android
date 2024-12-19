package com.x8bit.bitwarden.data.vault.repository

import android.net.Uri
import com.bitwarden.core.DateTime
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.Send
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUpdatedUserStateJson
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.util.isNoConnectionError
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.data.platform.repository.util.map
import com.x8bit.bitwarden.data.platform.repository.util.mapNullable
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndLoggedIn
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndUnlocked
import com.x8bit.bitwarden.data.platform.repository.util.updateToPendingOrLoading
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateFileSendResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateSendJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateFolderResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.network.service.FolderService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SendsService
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.sortAlphabetically
import com.x8bit.bitwarden.data.vault.repository.util.toDomainsData
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkSend
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSend
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Clock
import java.time.temporal.ChronoUnit

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 1000L

/**
 * Default implementation of [VaultRepository].
 */
@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
class VaultRepositoryImpl(
    private val syncService: SyncService,
    private val ciphersService: CiphersService,
    private val sendsService: SendsService,
    private val folderService: FolderService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val cipherManager: CipherManager,
    private val fileManager: FileManager,
    private val vaultLockManager: VaultLockManager,
    private val totpCodeManager: TotpCodeManager,
    private val userLogoutManager: UserLogoutManager,
    private val databaseSchemeManager: DatabaseSchemeManager,
    pushManager: PushManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
    private val reviewPromptManager: ReviewPromptManager,
) : VaultRepository,
    CipherManager by cipherManager,
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

    private val mutableDomainsStateFlow =
        MutableStateFlow<DataState<DomainsData>>(DataState.Loading)

    override var vaultFilterType: VaultFilterType = VaultFilterType.AllVaults

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
                    fido2CredentialAutofillViewList = null,
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

    override val domainsStateFlow: StateFlow<DataState<DomainsData>>
        get() = mutableDomainsStateFlow.asStateFlow()

    override val foldersStateFlow: StateFlow<DataState<List<FolderView>>>
        get() = mutableFoldersStateFlow.asStateFlow()

    override val collectionsStateFlow: StateFlow<DataState<List<CollectionView>>>
        get() = mutableCollectionsStateFlow.asStateFlow()

    override val sendDataStateFlow: StateFlow<DataState<SendData>>
        get() = mutableSendDataStateFlow.asStateFlow()

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
        mutableCiphersStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeVaultDiskCiphers(activeUserId)
            }
            .launchIn(unconfinedScope)

        // Setup domains MutableStateFlow
        mutableDomainsStateFlow
            .observeWhenSubscribedAndLoggedIn(
                userStateFlow = authDiskSource.userStateFlow,
            ) { activeUserId ->
                observeVaultDiskDomains(activeUserId)
            }
            .launchIn(unconfinedScope)
        // Setup folders MutableStateFlow
        mutableFoldersStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeVaultDiskFolders(activeUserId)
            }
            .launchIn(unconfinedScope)
        // Setup collections MutableStateFlow
        mutableCollectionsStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeVaultDiskCollections(activeUserId)
            }
            .launchIn(unconfinedScope)
        // Setup sends MutableStateFlow
        mutableSendDataStateFlow
            .observeWhenSubscribedAndUnlocked(
                authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeVaultDiskSends(activeUserId)
            }
            .launchIn(unconfinedScope)

        pushManager
            .fullSyncFlow
            .onEach { syncIfNecessary() }
            .launchIn(unconfinedScope)

        pushManager
            .syncCipherDeleteFlow
            .onEach(::deleteCipher)
            .launchIn(unconfinedScope)

        pushManager
            .syncCipherUpsertFlow
            .onEach(::syncCipherIfNecessary)
            .launchIn(ioScope)

        pushManager
            .syncSendDeleteFlow
            .onEach(::deleteSend)
            .launchIn(unconfinedScope)

        pushManager
            .syncSendUpsertFlow
            .onEach(::syncSendIfNecessary)
            .launchIn(ioScope)

        pushManager
            .syncFolderDeleteFlow
            .onEach(::deleteFolder)
            .launchIn(unconfinedScope)

        pushManager
            .syncFolderUpsertFlow
            .onEach(::syncFolderIfNecessary)
            .launchIn(ioScope)

        databaseSchemeManager
            .databaseSchemeChangeFlow
            .onEach { sync(forced = true) }
            .launchIn(ioScope)
    }

    private fun clearUnlockedData() {
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

    override fun sync(forced: Boolean) {
        val userId = activeUserId ?: return
        if (!syncJob.isCompleted) return
        mutableCiphersStateFlow.updateToPendingOrLoading()
        mutableDomainsStateFlow.updateToPendingOrLoading()
        mutableFoldersStateFlow.updateToPendingOrLoading()
        mutableCollectionsStateFlow.updateToPendingOrLoading()
        mutableSendDataStateFlow.updateToPendingOrLoading()
        syncJob = ioScope.launch { syncInternal(userId = userId, forced = forced) }
    }

    @Suppress("MagicNumber")
    override fun syncIfNecessary() {
        val userId = activeUserId ?: return
        val currentInstant = clock.instant()
        val lastSyncInstant = settingsDiskSource.getLastSyncTime(userId = userId)

        // Sync if we have never done so, the last time was at last 30 minutes ago, or the database
        // scheme changed since the last sync.
        if (lastSyncInstant == null ||
            currentInstant.isAfter(lastSyncInstant.plus(30, ChronoUnit.MINUTES))
        ) {
            sync()
        }
    }

    override suspend fun syncForResult(): SyncVaultDataResult {
        val userId = activeUserId ?: return SyncVaultDataResult.Error(throwable = null)
        syncJob = ioScope
            .async { syncInternal(userId = userId, forced = false) }
            .also {
                return try {
                    it.await()
                } catch (e: CancellationException) {
                    SyncVaultDataResult.Error(throwable = e)
                }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>> {
        val userId = activeUserId ?: return MutableStateFlow(
            DataState.Error(IllegalStateException("No active user"), null),
        )
        return vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData
                        .cipherViewList
                        .filter {
                            it.type == CipherType.LOGIN &&
                                !it.login?.totp.isNullOrBlank() &&
                                it.deletedDate == null
                        }
                        .toFilteredList(vaultFilterType)
                }
            }
            .flatMapLatest { cipherDataState ->
                val cipherList = cipherDataState.data ?: emptyList()
                totpCodeManager
                    .getTotpCodesStateFlow(
                        userId = userId,
                        cipherList = cipherList,
                    )
                    .map { verificationCodeDataStates ->
                        combineDataStates(
                            verificationCodeDataStates,
                            cipherDataState,
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
            DataState.Error(IllegalStateException("No active user"), null),
        )
        return getVaultItemStateFlow(cipherId)
            .flatMapLatest { cipherDataState ->
                val cipher = cipherDataState.data
                    ?: return@flatMapLatest flowOf(DataState.Loaded(null))
                totpCodeManager
                    .getTotpCodeStateFlow(
                        userId = userId,
                        cipher = cipher,
                    )
                    .map { totpCodeDataState ->
                        combineDataStates(
                            totpCodeDataState,
                            cipherDataState,
                        ) { _, _ ->
                            // We are only combining the DataStates to know the overall state,
                            // we map it to the appropriate value below.
                        }
                            .mapNullable { totpCodeDataState.data }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DataState.Loading,
            )
    }

    override suspend fun getDecryptedFido2CredentialAutofillViews(
        cipherViewList: List<CipherView>,
    ): DecryptFido2CredentialAutofillViewResult {
        return vaultSdkSource
            .decryptFido2CredentialAutofillViews(
                userId = activeUserId ?: return DecryptFido2CredentialAutofillViewResult.Error,
                cipherViews = cipherViewList.toTypedArray(),
            )
            .fold(
                onFailure = { DecryptFido2CredentialAutofillViewResult.Error },
                onSuccess = { DecryptFido2CredentialAutofillViewResult.Success(it) },
            )
    }

    override suspend fun silentlyDiscoverCredentials(
        userId: String,
        fido2CredentialStore: Fido2CredentialStore,
        relyingPartyId: String,
    ): Result<List<Fido2CredentialAutofillView>> =
        vaultSdkSource
            .silentlyDiscoverCredentials(
                userId = userId,
                fido2CredentialStore = fido2CredentialStore,
                relyingPartyId = relyingPartyId,
            )

    override fun emitTotpCodeResult(totpCodeResult: TotpCodeResult) {
        mutableTotpCodeResultFlow.tryEmit(totpCodeResult)
    }

    override suspend fun unlockVaultWithDecryptedUserKey(
        userId: String,
        decryptedUserKey: String,
    ): VaultUnlockResult = unlockVaultForUser(
        userId = userId,
        initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
            decryptedUserKey = decryptedUserKey,
        ),
    )

    override suspend fun unlockVaultWithBiometrics(): VaultUnlockResult {
        val userId = activeUserId ?: return VaultUnlockResult.InvalidStateError
        val biometricsKey = authDiskSource
            .getUserBiometricUnlockKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError
        return unlockVaultForUser(
            userId = userId,
            initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                decryptedUserKey = biometricsKey,
            ),
        )
            .also {
                if (it is VaultUnlockResult.Success) {
                    deriveTemporaryPinProtectedUserKeyIfNecessary(userId = userId)
                }
            }
    }

    override suspend fun unlockVaultWithMasterPassword(
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
                    deriveTemporaryPinProtectedUserKeyIfNecessary(userId = userId)
                }
            }
    }

    override suspend fun unlockVaultWithPin(
        pin: String,
    ): VaultUnlockResult {
        val userId = activeUserId ?: return VaultUnlockResult.InvalidStateError
        val pinProtectedUserKey = authDiskSource.getPinProtectedUserKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError
        return unlockVaultForUser(
            userId = userId,
            initUserCryptoMethod = InitUserCryptoMethod.Pin(
                pin = pin,
                pinProtectedUserKey = pinProtectedUserKey,
            ),
        )
    }

    override suspend fun createSend(
        sendView: SendView,
        fileUri: Uri?,
    ): CreateSendResult {
        val userId = activeUserId ?: return CreateSendResult.Error(message = null)
        return vaultSdkSource
            .encryptSend(
                userId = userId,
                sendView = sendView,
            )
            .flatMap { send ->
                when (send.type) {
                    SendType.TEXT -> sendsService.createTextSend(send.toEncryptedNetworkSend())
                    SendType.FILE -> createFileSend(fileUri, userId, send)
                }
            }
            .map { createSendResponse ->
                when (createSendResponse) {
                    is CreateSendJsonResponse.Invalid -> {
                        return CreateSendResult.Error(
                            message = createSendResponse.firstValidationErrorMessage,
                        )
                    }

                    is CreateSendJsonResponse.Success -> {
                        // Save the send immediately, regardless of whether the decrypt succeeds
                        vaultDiskSource.saveSend(userId = userId, send = createSendResponse.send)
                        createSendResponse
                    }
                }
            }
            .flatMap { createSendSuccessResponse ->
                vaultSdkSource.decryptSend(
                    userId = userId,
                    send = createSendSuccessResponse.send.toEncryptedSdkSend(),
                )
            }
            .fold(
                onFailure = { CreateSendResult.Error(message = null) },
                onSuccess = {
                    reviewPromptManager.registerCreateSendAction()
                    CreateSendResult.Success(it)
                },
            )
    }

    override suspend fun updateSend(
        sendId: String,
        sendView: SendView,
    ): UpdateSendResult {
        val userId = activeUserId ?: return UpdateSendResult.Error(null)
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
        val userId = activeUserId ?: return RemovePasswordSendResult.Error(null)
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

    override suspend fun deleteSend(sendId: String): DeleteSendResult {
        val userId = activeUserId ?: return DeleteSendResult.Error
        return sendsService
            .deleteSend(sendId)
            .onSuccess { vaultDiskSource.deleteSend(userId, sendId) }
            .fold(
                onSuccess = { DeleteSendResult.Success },
                onFailure = { DeleteSendResult.Error },
            )
    }

    override suspend fun generateTotp(
        totpCode: String,
        time: DateTime,
    ): GenerateTotpResult {
        val userId = activeUserId ?: return GenerateTotpResult.Error
        return vaultSdkSource.generateTotp(
            time = time,
            userId = userId,
            totp = totpCode,
        )
            .fold(
                onSuccess = {
                    GenerateTotpResult.Success(
                        code = it.code,
                        periodSeconds = it.period.toInt(),
                    )
                },
                onFailure = { GenerateTotpResult.Error },
            )
    }

    override suspend fun createFolder(folderView: FolderView): CreateFolderResult {
        val userId = activeUserId ?: return CreateFolderResult.Error
        return vaultSdkSource
            .encryptFolder(
                userId = userId,
                folder = folderView,
            )
            .flatMap { folder ->
                folderService
                    .createFolder(
                        body = folder.toEncryptedNetworkFolder(),
                    )
            }
            .onSuccess { vaultDiskSource.saveFolder(userId = userId, folder = it) }
            .flatMap { vaultSdkSource.decryptFolder(userId, it.toEncryptedSdkFolder()) }
            .fold(
                onSuccess = { CreateFolderResult.Success(folderView = it) },
                onFailure = { CreateFolderResult.Error },
            )
    }

    override suspend fun updateFolder(
        folderId: String,
        folderView: FolderView,
    ): UpdateFolderResult {
        val userId = activeUserId ?: return UpdateFolderResult.Error(null)
        return vaultSdkSource
            .encryptFolder(
                userId = userId,
                folder = folderView,
            )
            .flatMap { folder ->
                folderService
                    .updateFolder(
                        folderId = folder.id.toString(),
                        body = folder.toEncryptedNetworkFolder(),
                    )
            }
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is UpdateFolderResponseJson.Success -> {
                            vaultDiskSource.saveFolder(userId, response.folder)
                            vaultSdkSource
                                .decryptFolder(
                                    userId,
                                    response.folder.toEncryptedSdkFolder(),
                                )
                                .fold(
                                    onSuccess = { UpdateFolderResult.Success(it) },
                                    onFailure = { UpdateFolderResult.Error(errorMessage = null) },
                                )
                        }

                        is UpdateFolderResponseJson.Invalid -> {
                            UpdateFolderResult.Error(response.message)
                        }
                    }
                },
                onFailure = { UpdateFolderResult.Error(it.message) },
            )
    }

    override suspend fun deleteFolder(folderId: String): DeleteFolderResult {
        val userId = activeUserId ?: return DeleteFolderResult.Error
        return folderService
            .deleteFolder(
                folderId = folderId,
            )
            .onSuccess {
                clearFolderIdFromCiphers(folderId, userId)
                vaultDiskSource.deleteFolder(userId, folderId)
            }
            .fold(
                onSuccess = { DeleteFolderResult.Success },
                onFailure = { DeleteFolderResult.Error },
            )
    }

    private suspend fun clearFolderIdFromCiphers(folderId: String, userId: String) {
        vaultDiskSource.getCiphers(userId).firstOrNull()?.forEach {
            if (it.folderId == folderId) {
                vaultDiskSource.saveCipher(
                    userId, it.copy(folderId = null),
                )
            }
        }
    }

    override suspend fun exportVaultDataToString(format: ExportFormat): ExportVaultDataResult {
        val userId = activeUserId ?: return ExportVaultDataResult.Error
        val folders = vaultDiskSource
            .getFolders(userId)
            .firstOrNull()
            .orEmpty()
            .map { it.toEncryptedSdkFolder() }

        val ciphers = vaultDiskSource
            .getCiphers(userId)
            .firstOrNull()
            .orEmpty()
            .map { it.toEncryptedSdkCipher() }
            .filter { it.collectionIds.isEmpty() && it.deletedDate == null }

        return vaultSdkSource
            .exportVaultDataToString(
                userId = userId,
                folders = folders,
                ciphers = ciphers,
                format = format,
            )
            .fold(
                onSuccess = { ExportVaultDataResult.Success(it) },
                onFailure = { ExportVaultDataResult.Error },
            )
    }

    /**
     * Checks if the given [userId] has an associated encrypted PIN key but not a pin-protected user
     * key. This indicates a scenario in which a user has requested PIN unlocking but requires
     * master-password unlocking on app restart. This function may then be called after such an
     * unlock to derive a pin-protected user key and store it in memory for use for any subsequent
     * unlocks during this current app session.
     *
     * If the user's vault has not yet been unlocked, this call will do nothing.
     */
    private suspend fun deriveTemporaryPinProtectedUserKeyIfNecessary(userId: String) {
        val encryptedPin = authDiskSource.getEncryptedPin(userId = userId) ?: return
        val existingPinProtectedUserKey = authDiskSource.getPinProtectedUserKey(userId = userId)
        if (existingPinProtectedUserKey != null) return
        vaultSdkSource
            .derivePinProtectedUserKey(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            .onSuccess { pinProtectedUserKey ->
                authDiskSource.storePinProtectedUserKey(
                    userId = userId,
                    pinProtectedUserKey = pinProtectedUserKey,
                    inMemoryOnly = true,
                )
            }
    }

    private fun storeProfileData(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val userId = profile.id
        authDiskSource.apply {
            storeUserKey(
                userId = userId,
                userKey = profile.key,
            )
            storePrivateKey(
                userId = userId,
                privateKey = profile.privateKey,
            )
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
            storeOrganizations(
                userId = userId,
                organizations = profile.organizations,
            )
        }
    }

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
            .onStart { mutableCiphersStateFlow.updateToPendingOrLoading() }
            .map {
                waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptCipherList(
                        userId = userId,
                        cipherList = it.toEncryptedSdkCipherList(),
                    )
                    .fold(
                        onSuccess = { ciphers -> DataState.Loaded(ciphers.sortAlphabetically()) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .map { it.orLoadingIfNotSynced(userId = userId) }
            .onEach { mutableCiphersStateFlow.value = it }

    private fun observeVaultDiskDomains(
        userId: String,
    ): Flow<DataState<DomainsData>> =
        vaultDiskSource
            .getDomains(userId = userId)
            .onStart { mutableDomainsStateFlow.updateToPendingOrLoading() }
            .map {
                DataState.Loaded(
                    data = it.toDomainsData(),
                )
            }
            .onEach { mutableDomainsStateFlow.value = it }

    private fun observeVaultDiskFolders(
        userId: String,
    ): Flow<DataState<List<FolderView>>> =
        vaultDiskSource
            .getFolders(userId = userId)
            .onStart { mutableFoldersStateFlow.updateToPendingOrLoading() }
            .map {
                waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptFolderList(
                        userId = userId,
                        folderList = it.toEncryptedSdkFolderList(),
                    )
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
                waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptCollectionList(
                        userId = userId,
                        collectionList = it.toEncryptedSdkCollectionList(),
                    )
                    .fold(
                        onSuccess = { collections ->
                            DataState.Loaded(
                                collections.sortAlphabetically(),
                            )
                        },
                        onFailure = { throwable -> DataState.Error(throwable) },
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
                waitUntilUnlocked(userId = userId)
                vaultSdkSource
                    .decryptSendList(
                        userId = userId,
                        sendList = it.toEncryptedSdkSendList(),
                    )
                    .fold(
                        onSuccess = { sends -> DataState.Loaded(sends.sortAlphabetically()) },
                        onFailure = { throwable -> DataState.Error(throwable) },
                    )
            }
            .map { it.orLoadingIfNotSynced(userId = userId) }
            .map { dataState -> dataState.map { SendData(it) } }
            .onEach { mutableSendDataStateFlow.value = it }

    private fun updateVaultStateFlowsToError(throwable: Throwable) {
        mutableCiphersStateFlow.update { currentState ->
            throwable.toNetworkOrErrorState(
                data = currentState.data,
            )
        }
        mutableDomainsStateFlow.update { currentState ->
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
            .takeUnless {
                settingsDiskSource.getLastSyncTime(userId = userId) == null
            }
            ?: DataState.Loading

    //region Push notification helpers
    /**
     * Deletes the cipher specified by [syncCipherDeleteData] from disk.
     */
    private suspend fun deleteCipher(syncCipherDeleteData: SyncCipherDeleteData) {
        val userId = activeUserId ?: return

        val cipherId = syncCipherDeleteData.cipherId
        vaultDiskSource.deleteCipher(
            userId = userId,
            cipherId = cipherId,
        )
    }

    /**
     * Syncs an individual cipher contained in [syncCipherUpsertData] to disk if certain criteria
     * are met. If the resource cannot be found cloud-side, and it was updated, delete it from disk
     * for now.
     */
    private suspend fun syncCipherIfNecessary(syncCipherUpsertData: SyncCipherUpsertData) {
        val userId = activeUserId ?: return
        val cipherId = syncCipherUpsertData.cipherId
        val organizationId = syncCipherUpsertData.organizationId
        val collectionIds = syncCipherUpsertData.collectionIds
        val revisionDate = syncCipherUpsertData.revisionDate
        val isUpdate = syncCipherUpsertData.isUpdate

        val localCipher = ciphersStateFlow
            .mapNotNull { it.data }
            .first()
            .find { it.id == cipherId }

        // Return if local cipher is more recent
        if (localCipher != null &&
            localCipher.revisionDate.epochSecond > revisionDate.toEpochSecond()
        ) {
            return
        }

        var shouldUpdate: Boolean
        val shouldCheckCollections: Boolean

        when {
            isUpdate -> {
                shouldUpdate = localCipher != null
                shouldCheckCollections = true
            }

            collectionIds == null || organizationId == null -> {
                shouldUpdate = localCipher == null
                shouldCheckCollections = false
            }

            else -> {
                shouldUpdate = false
                shouldCheckCollections = true
            }
        }

        if (!shouldUpdate && shouldCheckCollections && organizationId != null) {
            // Check if there are any collections in common
            shouldUpdate = collectionsStateFlow
                .mapNotNull { it.data }
                .first()
                .mapNotNull { it.id }
                .any { collectionIds?.contains(it) == true } == true
        }

        if (!shouldUpdate) return

        ciphersService
            .getCipher(cipherId)
            .fold(
                onSuccess = { vaultDiskSource.saveCipher(userId, it) },
                onFailure = {
                    // Delete any updates if it's missing from the server
                    val httpException = it as? HttpException
                    @Suppress("MagicNumber")
                    if (httpException?.code() == 404 && isUpdate) {
                        vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId)
                    }
                },
            )
    }

    private suspend fun createFileSend(
        uri: Uri?,
        userId: String,
        send: Send,
    ): Result<CreateSendJsonResponse> {
        uri ?: return IllegalArgumentException(
            "File URI must be present to create a File Send.",
        )
            .asFailure()

        return fileManager
            .writeUriToCache(uri)
            .flatMap { file ->
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = send,
                    path = file.absolutePath,
                    destinationFilePath = file.absolutePath,
                )
            }
            .flatMap { encryptedFile ->
                sendsService
                    .createFileSend(
                        body = send.toEncryptedNetworkSend(
                            fileLength = encryptedFile.length(),
                        ),
                    )
                    .flatMap { sendFileResponse ->
                        when (sendFileResponse) {
                            is CreateFileSendResponse.Invalid -> {
                                CreateSendJsonResponse
                                    .Invalid(
                                        message = sendFileResponse.message,
                                        validationErrors = sendFileResponse.validationErrors,
                                    )
                                    .asSuccess()
                            }

                            is CreateFileSendResponse.Success -> {
                                sendsService
                                    .uploadFile(
                                        sendFileResponse = sendFileResponse.createFileJsonResponse,
                                        encryptedFile = encryptedFile,
                                    )
                                    .also {
                                        // Delete encrypted file once it has been uploaded.
                                        fileManager.delete(encryptedFile)
                                    }
                                    .map { CreateSendJsonResponse.Success(it) }
                            }
                        }
                    }
            }
    }

    /**
     * Deletes the send specified by [syncSendDeleteData] from disk.
     */
    private suspend fun deleteSend(syncSendDeleteData: SyncSendDeleteData) {
        val userId = activeUserId ?: return

        val sendId = syncSendDeleteData.sendId
        vaultDiskSource.deleteSend(
            userId = userId,
            sendId = sendId,
        )
    }

    /**
     * Syncs an individual send contained in [syncSendUpsertData] to disk if certain criteria are
     * met. If the resource cannot be found cloud-side, and it was updated, delete it from disk for
     * now.
     */
    private suspend fun syncSendIfNecessary(syncSendUpsertData: SyncSendUpsertData) {
        val userId = activeUserId ?: return
        val sendId = syncSendUpsertData.sendId
        val isUpdate = syncSendUpsertData.isUpdate
        val revisionDate = syncSendUpsertData.revisionDate

        val localSend = sendDataStateFlow
            .mapNotNull { it.data }
            .first()
            .sendViewList
            .find { it.id == sendId }
        val isValidCreate = !isUpdate && localSend == null
        val isValidUpdate = isUpdate &&
            localSend != null &&
            localSend.revisionDate.epochSecond < revisionDate.toEpochSecond()

        if (!isValidCreate && !isValidUpdate) return

        sendsService
            .getSend(sendId)
            .fold(
                onSuccess = { vaultDiskSource.saveSend(userId, it) },
                onFailure = {
                    // Delete any updates if it's missing from the server
                    val httpException = it as? HttpException
                    @Suppress("MagicNumber")
                    if (httpException?.code() == 404 && isUpdate) {
                        vaultDiskSource.deleteSend(userId = userId, sendId = sendId)
                    }
                },
            )
    }

    /**
     * Deletes the folder specified by [syncFolderDeleteData] from disk.
     */
    private suspend fun deleteFolder(syncFolderDeleteData: SyncFolderDeleteData) {
        val userId = activeUserId ?: return

        val folderId = syncFolderDeleteData.folderId
        clearFolderIdFromCiphers(
            folderId = folderId,
            userId = userId,
        )
        vaultDiskSource.deleteFolder(
            folderId = folderId,
            userId = userId,
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

        val localFolder = foldersStateFlow
            .mapNotNull { it.data }
            .first()
            .find { it.id == folderId }
        val isValidCreate = !isUpdate && localFolder == null
        val isValidUpdate = isUpdate &&
            localFolder != null &&
            localFolder.revisionDate.epochSecond < revisionDate.toEpochSecond()

        if (!isValidCreate && !isValidUpdate) return

        folderService
            .getFolder(folderId)
            .onSuccess { vaultDiskSource.saveFolder(userId, it) }
    }
    //endregion Push Notification helpers

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
                syncService
                    .getAccountRevisionDateMillis()
                    .fold(
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
                                    .getCiphers(userId)
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

        return syncService
            .sync()
            .fold(
                onSuccess = { syncResponse ->
                    val localSecurityStamp = authDiskSource.userState?.activeAccount?.profile?.stamp
                    val serverSecurityStamp = syncResponse.profile.securityStamp

                    // Log the user out if the stamps do not match
                    localSecurityStamp?.let {
                        if (serverSecurityStamp != localSecurityStamp) {
                            userLogoutManager.softLogout(userId = userId, isExpired = true)
                            return SyncVaultDataResult.Error(throwable = null)
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
                },
                onFailure = { throwable ->
                    updateVaultStateFlowsToError(throwable = throwable)
                    SyncVaultDataResult.Error(throwable = throwable)
                },
            )
    }
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
