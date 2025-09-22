package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.collections.CollectionView
import com.bitwarden.core.DateTime
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.core.data.repository.util.map
import com.bitwarden.core.data.repository.util.mapNullable
import com.bitwarden.core.data.repository.util.updateToPendingOrLoading
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.network.util.isNoConnectionError
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.MissingPropertyException
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.DatabaseSchemeManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndLoggedIn
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndUnlocked
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.CredentialExchangeImportManager
import com.x8bit.bitwarden.data.vault.manager.FolderManager
import com.x8bit.bitwarden.data.vault.manager.SendManager
import com.x8bit.bitwarden.data.vault.manager.TotpCodeManager
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.DomainsData
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.sortAlphabetically
import com.x8bit.bitwarden.data.vault.repository.util.sortAlphabeticallyByTypeAndOrganization
import com.x8bit.bitwarden.data.vault.repository.util.toDomainsData
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipherList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCollectionList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolderList
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSendList
import com.x8bit.bitwarden.data.vault.repository.util.toSdkAccount
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.GeneralSecurityException
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.crypto.Cipher

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
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val cipherManager: CipherManager,
    private val folderManager: FolderManager,
    private val sendManager: SendManager,
    private val vaultLockManager: VaultLockManager,
    private val totpCodeManager: TotpCodeManager,
    databaseSchemeManager: DatabaseSchemeManager,
    pushManager: PushManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
    private val vaultSyncManager: VaultSyncManager,
    private val credentialExchangeImportManager: CredentialExchangeImportManager,
) : VaultRepository,
    CipherManager by cipherManager,
    FolderManager by folderManager,
    SendManager by sendManager,
    VaultLockManager by vaultLockManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private var syncJob: Job = Job().apply { complete() }

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableTotpCodeResultFlow = bufferedMutableSharedFlow<TotpCodeResult>()

    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)

    private val mutableDecryptCipherListResultFlow =
        MutableStateFlow<DataState<DecryptCipherListResult>>(DataState.Loading)

    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<List<FolderView>>>(DataState.Loading)

    private val mutableCollectionsStateFlow =
        MutableStateFlow<DataState<List<CollectionView>>>(DataState.Loading)

    private val mutableDomainsStateFlow =
        MutableStateFlow<DataState<DomainsData>>(DataState.Loading)

    override var vaultFilterType: VaultFilterType = VaultFilterType.AllVaults

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

    override val totpCodeFlow: Flow<TotpCodeResult>
        get() = mutableTotpCodeResultFlow.asSharedFlow()

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
                vaultUnlockFlow = vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeVaultDiskCiphersToCipherListView(activeUserId)
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
            .onEach { sync(forced = false) }
            .launchIn(unconfinedScope)

        databaseSchemeManager
            .databaseSchemeChangeFlow
            .onEach { sync(forced = true) }
            .launchIn(ioScope)
    }

    private fun clearUnlockedData() {
        mutableDecryptCipherListResultFlow.update { DataState.Loading }
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
        mutableDecryptCipherListResultFlow.updateToPendingOrLoading()
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
            sync(forced = false)
        }
    }

    override suspend fun syncForResult(): SyncVaultDataResult {
        val userId = activeUserId
            ?: return SyncVaultDataResult.Error(throwable = NoActiveUserException())
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
                    val getCipherResult = vaultData
                        .decryptCipherListResult
                        .successes
                        .find { it.id == itemId }
                        .let { getCipher(itemId) }
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

    override fun getVaultListItemStateFlow(itemId: String): StateFlow<DataState<CipherListView?>> =
        vaultDataStateFlow
            .map { dataState ->
                dataState.map { vaultData ->
                    vaultData
                        .decryptCipherListResult
                        .successes
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
                        .decryptCipherListResult
                        .successes
                        .filter {
                            it.type is CipherListViewType.Login &&
                                !it.login?.totp.isNullOrBlank() &&
                                it.deletedDate == null
                        }
                        .toFilteredList(vaultFilterType)
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
        return getVaultListItemStateFlow(cipherId)
            .flatMapLatest { cipherDataState ->
                cipherDataState
                    .data
                    ?.let {
                        totpCodeManager
                            .getTotpCodeStateFlow(userId = userId, cipherListView = it)
                            .map { totpCodeDataState ->
                                combineDataStates(totpCodeDataState, cipherDataState) { _, _ ->
                                    // We are only combining the DataStates to know the overall
                                    // state, we map it to the appropriate value below.
                                }
                                    .mapNullable { totpCodeDataState.data }
                            }
                    }
                    ?: flowOf(DataState.Loaded(null))
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DataState.Loading,
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

    override suspend fun unlockVaultWithBiometrics(cipher: Cipher): VaultUnlockResult {
        val userId = activeUserId
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())
        val biometricsKey = authDiskSource
            .getUserBiometricUnlockKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                error = MissingPropertyException("Biometric key"),
            )
        val iv = authDiskSource.getUserBiometricInitVector(userId = userId)
        val decryptedUserKey = iv
            ?.let {
                try {
                    cipher
                        .doFinal(biometricsKey.toByteArray(Charsets.ISO_8859_1))
                        .decodeToString()
                } catch (e: GeneralSecurityException) {
                    Timber.w(e, "unlockVaultWithBiometrics failed when decrypting biometrics key")
                    return VaultUnlockResult.BiometricDecodingError(error = e)
                }
            }
            ?: biometricsKey
        val encryptedBiometricsKey = if (iv == null) {
            // Attempting to setup an encrypted pin before unlocking, if this fails we send back
            // the biometrics error and users will need to sign in another way and re-setup
            // biometrics.
            try {
                cipher
                    .doFinal(biometricsKey.encodeToByteArray())
                    .toString(Charsets.ISO_8859_1)
            } catch (e: GeneralSecurityException) {
                Timber.w(e, "unlockVaultWithBiometrics failed to migrate the user to IV encryption")
                return VaultUnlockResult.BiometricDecodingError(error = e)
            }
        } else {
            null
        }
        return this
            .unlockVaultForUser(
                userId = userId,
                initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                    decryptedUserKey = decryptedUserKey,
                ),
            )
            .also {
                if (it is VaultUnlockResult.Success) {
                    encryptedBiometricsKey?.let { key ->
                        // If this key is present, we store it and the associated IV for future use
                        // since we want to migrate the user to a more secure form of biometrics.
                        authDiskSource.storeUserBiometricUnlockKey(
                            userId = userId,
                            biometricsKey = key,
                        )
                        authDiskSource.storeUserBiometricInitVector(userId = userId, iv = cipher.iv)
                    }
                    deriveTemporaryPinProtectedUserKeyIfNecessary(userId = userId)
                }
            }
    }

    override suspend fun unlockVaultWithMasterPassword(
        masterPassword: String,
    ): VaultUnlockResult {
        val userId = activeUserId
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())
        val userKey = authDiskSource.getUserKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                error = MissingPropertyException("User key"),
            )
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
        val userId = activeUserId
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())
        val pinProtectedUserKey = authDiskSource.getPinProtectedUserKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                error = MissingPropertyException("Pin protected key"),
            )
        return unlockVaultForUser(
            userId = userId,
            initUserCryptoMethod = InitUserCryptoMethod.Pin(
                pin = pin,
                pinProtectedUserKey = pinProtectedUserKey,
            ),
        )
    }

    override suspend fun generateTotp(
        cipherId: String,
        time: DateTime,
    ): GenerateTotpResult {
        val userId = activeUserId
            ?: return GenerateTotpResult.Error(error = NoActiveUserException())
        val cipherListView = decryptCipherListResultStateFlow
            .value
            .data
            ?.successes
            ?.find { it.id == cipherId }
            ?: return GenerateTotpResult.Error(
                error = IllegalArgumentException(cipherId),
            )

        return vaultSdkSource.generateTotpForCipherListView(
            time = time,
            userId = userId,
            cipherListView = cipherListView,
        )
            .fold(
                onSuccess = {
                    GenerateTotpResult.Success(
                        code = it.code,
                        periodSeconds = it.period.toInt(),
                    )
                },
                onFailure = { GenerateTotpResult.Error(error = it) },
            )
    }

    override suspend fun exportVaultDataToString(
        format: ExportFormat,
        restrictedTypes: List<CipherType>,
    ): ExportVaultDataResult {
        val userId = activeUserId
            ?: return ExportVaultDataResult.Error(error = NoActiveUserException())
        val folders = vaultDiskSource
            .getFolders(userId)
            .firstOrNull()
            .orEmpty()
            .map { it.toEncryptedSdkFolder() }

        val ciphers = vaultDiskSource
            .getCiphersFlow(userId)
            .firstOrNull()
            .orEmpty()
            .map { it.toEncryptedSdkCipher() }
            .filter {
                it.collectionIds.isEmpty() &&
                    it.deletedDate == null &&
                    !restrictedTypes.contains(it.type)
            }

        return vaultSdkSource
            .exportVaultDataToString(
                userId = userId,
                folders = folders,
                ciphers = ciphers,
                format = format,
            )
            .fold(
                onSuccess = { ExportVaultDataResult.Success(it) },
                onFailure = { ExportVaultDataResult.Error(error = it) },
            )
    }

    override suspend fun importCxfPayload(
        payload: String,
    ): ImportCredentialsResult {
        val userId = activeUserId
            ?: return ImportCredentialsResult.Error(error = NoActiveUserException())
        val importResult = credentialExchangeImportManager
            .importCxfPayload(
                userId = userId,
                payload = payload,
            )
        return when (importResult) {
            is ImportCxfPayloadResult.Error -> {
                ImportCredentialsResult.Error(error = importResult.error)
            }

            ImportCxfPayloadResult.NoItems -> {
                ImportCredentialsResult.NoItems
            }

            is ImportCxfPayloadResult.Success -> {
                when (val syncResult = syncInternal(userId = userId, forced = true)) {
                    is SyncVaultDataResult.Error -> {
                        ImportCredentialsResult.SyncFailed(error = syncResult.throwable)
                    }

                    is SyncVaultDataResult.Success -> {
                        ImportCredentialsResult.Success(itemCount = importResult.itemCount)
                    }
                }
            }
        }
    }

    override suspend fun exportVaultDataToCxf(
        ciphers: List<CipherListView>,
    ): Result<String> {
        val userId = activeUserId
            ?: return NoActiveUserException().asFailure()
        val account = authDiskSource.userState
            ?.activeAccount
            ?.toSdkAccount()
            ?: return NoActiveUserException().asFailure()

        val ciphers = vaultDiskSource
            .getSelectedCiphers(userId = userId, cipherIds = ciphers.mapNotNull { it.id })
            .map { it.toEncryptedSdkCipher() }

        return vaultSdkSource
            .exportVaultDataToCxf(
                userId = userId,
                account = account,
                ciphers = ciphers,
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

    private suspend fun unlockVaultForUser(
        userId: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val account = authDiskSource.userState?.accounts?.get(userId)
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())
        val accountKeys = authDiskSource.getAccountKeys(userId = userId)
        val privateKey = accountKeys
            ?.publicKeyEncryptionKeyPair
            ?.wrappedPrivateKey
            ?: authDiskSource.getPrivateKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                error = MissingPropertyException("Private key"),
            )
        val signingKey = accountKeys?.signatureKeyPair?.wrappedSigningKey
        val securityState = accountKeys?.securityState?.securityState
        val organizationKeys = authDiskSource
            .getOrganizationKeys(userId = userId)
        return unlockVault(
            userId = userId,
            email = account.profile.email,
            kdf = account.profile.toSdkParams(),
            privateKey = privateKey,
            signingKey = signingKey,
            securityState = securityState,
            initUserCryptoMethod = initUserCryptoMethod,
            organizationKeys = organizationKeys,
        )
    }

    private fun observeVaultDiskCiphersToCipherListView(
        userId: String,
    ): Flow<DataState<DecryptCipherListResult>> =
        vaultDiskSource
            .getCiphersFlow(userId = userId)
            .onStart { mutableDecryptCipherListResultFlow.updateToPendingOrLoading() }
            .map {
                waitUntilUnlocked(userId = userId)
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
                        onFailure = { throwable -> DataState.Error(throwable) },
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
                                collections.sortAlphabeticallyByTypeAndOrganization(
                                    userOrganizations = authDiskSource
                                        .getOrganizations(userId = userId)
                                        .orEmpty(),
                                ),
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
        mutableDecryptCipherListResultFlow.update { currentState ->
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

    private suspend fun syncInternal(
        userId: String,
        forced: Boolean,
    ): SyncVaultDataResult =
        vaultSyncManager
            .sync(userId = userId, forced = forced)
            .also { result ->
                if (result is SyncVaultDataResult.Error) {
                    updateVaultStateFlowsToError(throwable = result.throwable)
                }
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
