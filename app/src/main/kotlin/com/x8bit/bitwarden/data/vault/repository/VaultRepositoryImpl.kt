package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.DateTime
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.core.data.repository.util.map
import com.bitwarden.core.data.repository.util.mapNullable
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.exporters.ExportFormat
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.error.MissingPropertyException
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
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
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.methodName
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import com.x8bit.bitwarden.data.vault.repository.util.toSdkAccount
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.crypto.Cipher

/**
 * Default implementation of [VaultRepository].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class VaultRepositoryImpl(
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val cipherManager: CipherManager,
    private val folderManager: FolderManager,
    private val sendManager: SendManager,
    private val vaultLockManager: VaultLockManager,
    private val totpCodeManager: TotpCodeManager,
    private val vaultSyncManager: VaultSyncManager,
    private val credentialExchangeImportManager: CredentialExchangeImportManager,
    dispatcherManager: DispatcherManager,
) : VaultRepository,
    CipherManager by cipherManager,
    FolderManager by folderManager,
    SendManager by sendManager,
    VaultLockManager by vaultLockManager,
    VaultSyncManager by vaultSyncManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableTotpCodeResultFlow = bufferedMutableSharedFlow<TotpCodeResult>()

    override var vaultFilterType: VaultFilterType = VaultFilterType.AllVaults

    override val totpCodeFlow: Flow<TotpCodeResult>
        get() = mutableTotpCodeResultFlow.asSharedFlow()

    override fun deleteVaultData(userId: String) {
        ioScope.launch {
            vaultDiskSource.deleteVaultData(userId)
        }
    }

    override fun getVaultItemStateFlow(itemId: String): StateFlow<DataState<CipherView?>> =
        vaultSyncManager
            .vaultDataStateFlow
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
        vaultSyncManager
            .vaultDataStateFlow
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
        vaultSyncManager
            .vaultDataStateFlow
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>> {
        val userId = activeUserId ?: return MutableStateFlow(
            DataState.Error(IllegalStateException("No active user"), null),
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
        return this.getVaultListItemStateFlow(cipherId)
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
    ): VaultUnlockResult = this.unlockVaultForUser(
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
                    deriveTemporaryPinProtectedUserKeyIfNecessary(
                        userId = userId,
                        initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = decryptedUserKey,
                        ),
                    )
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
        return this
            .unlockVaultForUser(
                userId = userId,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
            )
            .also {
                if (it is VaultUnlockResult.Success) {
                    deriveTemporaryPinProtectedUserKeyIfNecessary(
                        userId = userId,
                        initUserCryptoMethod = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    )
                }
            }
    }

    override suspend fun unlockVaultWithPin(
        pin: String,
    ): VaultUnlockResult {
        val userId = activeUserId
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())

        return authDiskSource.getPinProtectedUserKeyEnvelope(userId = userId)
            ?.let { pinProtectedUserKeyEnvelope ->
                this.unlockVaultForUser(
                    userId = userId,
                    initUserCryptoMethod = InitUserCryptoMethod.PinEnvelope(
                        pin = pin,
                        pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                    ),
                )
            }
            ?: run {
                // This is needed to support unlocking with a legacy pin protected user key.
                // Once the vault is unlocked, the user's pin protected user key is migrated to
                // a pin protected user key envelope.
                val pinProtectedUserKey = authDiskSource.getPinProtectedUserKey(userId = userId)
                    ?: return VaultUnlockResult.InvalidStateError(
                        error = MissingPropertyException("Pin protected key"),
                    )

                this.unlockVaultForUser(
                    userId = userId,
                    initUserCryptoMethod = InitUserCryptoMethod.Pin(
                        pin = pin,
                        pinProtectedUserKey = pinProtectedUserKey,
                    ),
                )
            }
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

            is ImportCxfPayloadResult.SyncFailed -> {
                ImportCredentialsResult.SyncFailed(error = importResult.error)
            }

            is ImportCxfPayloadResult.Success -> {
                ImportCredentialsResult.Success(itemCount = importResult.itemCount)
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
     *
     * @param userId The ID of the user to check.
     * @param initUserCryptoMethod The method used to initialize the user's crypto.
     */
    private suspend fun deriveTemporaryPinProtectedUserKeyIfNecessary(
        userId: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ) {
        val encryptedPin = authDiskSource.getEncryptedPin(userId = userId) ?: return
        val existingPinProtectedUserKeyEnvelope = authDiskSource
            .getPinProtectedUserKeyEnvelope(
                userId = userId,
            )
        if (existingPinProtectedUserKeyEnvelope != null) return

        Timber.d("[Auth] Vault unlocked, method: ${initUserCryptoMethod.methodName()}")

        vaultSdkSource
            .enrollPinWithEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            .onSuccess { enrollPinResponse ->
                authDiskSource.storeEncryptedPin(
                    userId = userId,
                    encryptedPin = enrollPinResponse.userKeyEncryptedPin,
                )
                authDiskSource.storePinProtectedUserKeyEnvelope(
                    userId = userId,
                    pinProtectedUserKeyEnvelope = enrollPinResponse.pinProtectedUserKeyEnvelope,
                    inMemoryOnly = true,
                )
                authDiskSource.storePinProtectedUserKey(
                    userId = userId,
                    pinProtectedUserKey = null,
                    inMemoryOnly = true,
                )
                Timber.d("[Auth] Set PIN-protected user key in memory")
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
        return vaultLockManager.unlockVault(
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
}
