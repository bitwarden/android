package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Primary implementation [VaultLockManager].
 */
@Suppress("TooManyFunctions")
class VaultLockManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val settingsRepository: SettingsRepository,
    private val dispatcherManager: DispatcherManager,
) : VaultLockManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableVaultStateStateFlow =
        MutableStateFlow(
            VaultState(
                unlockedVaultUserIds = emptySet(),
                unlockingVaultUserIds = emptySet(),
            ),
        )

    override val vaultStateFlow: StateFlow<VaultState>
        get() = mutableVaultStateStateFlow.asStateFlow()

    init {
        observeVaultTimeoutChanges()
    }

    override fun isVaultUnlocked(userId: String): Boolean =
        userId in mutableVaultStateStateFlow.value.unlockedVaultUserIds

    override fun isVaultUnlocking(userId: String): Boolean =
        userId in mutableVaultStateStateFlow.value.unlockingVaultUserIds

    override fun lockVault(userId: String) {
        setVaultToLocked(userId = userId)
    }

    override fun lockVaultForCurrentUser() {
        activeUserId?.let {
            lockVault(it)
        }
    }

    override fun lockVaultIfNecessary(userId: String) {
        // Don't lock the vault for users with a Never Lock timeout.
        val hasNeverLockTimeout =
            settingsRepository.getVaultTimeoutStateFlow(userId = userId).value == VaultTimeout.Never
        if (hasNeverLockTimeout) return

        lockVault(userId = userId)
    }

    override suspend fun unlockVault(
        userId: String,
        email: String,
        kdf: Kdf,
        privateKey: String,
        initUserCryptoMethod: InitUserCryptoMethod,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult =
        flow {
            setVaultToUnlocking(userId = userId)
            emit(
                vaultSdkSource
                    .initializeCrypto(
                        userId = userId,
                        request = InitUserCryptoRequest(
                            kdfParams = kdf,
                            email = email,
                            privateKey = privateKey,
                            method = initUserCryptoMethod,
                        ),
                    )
                    .flatMap { result ->
                        // Initialize the SDK for organizations if necessary
                        if (organizationKeys != null &&
                            result is InitializeCryptoResult.Success
                        ) {
                            vaultSdkSource.initializeOrganizationCrypto(
                                userId = userId,
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
            .onCompletion { setVaultToNotUnlocking(userId = userId) }
            .first()

    private fun setVaultToUnlocked(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds + userId,
            )
        }
    }

    private fun setVaultToLocked(userId: String) {
        vaultSdkSource.clearCrypto(userId = userId)
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockedVaultUserIds = it.unlockedVaultUserIds - userId,
            )
        }
    }

    private fun setVaultToUnlocking(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockingVaultUserIds = it.unlockingVaultUserIds + userId,
            )
        }
    }

    private fun setVaultToNotUnlocking(userId: String) {
        mutableVaultStateStateFlow.update {
            it.copy(
                unlockingVaultUserIds = it.unlockingVaultUserIds - userId,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVaultTimeoutChanges() {
        authDiskSource
            .userStateFlow
            .map { userState -> userState?.accounts?.keys.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { userIds ->
                userIds
                    .map { userId -> vaultTimeoutChangesForUserFlow(userId = userId) }
                    .merge()
            }
            .launchIn(unconfinedScope)
    }

    private fun vaultTimeoutChangesForUserFlow(userId: String) =
        settingsRepository
            .getVaultTimeoutStateFlow(userId = userId)
            .onEach { vaultTimeout ->
                handleUserAutoUnlockChanges(
                    userId = userId,
                    vaultTimeout = vaultTimeout,
                )
            }

    private suspend fun handleUserAutoUnlockChanges(
        userId: String,
        vaultTimeout: VaultTimeout,
    ) {
        if (vaultTimeout != VaultTimeout.Never) {
            // Clear the user encryption keys
            authDiskSource.storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )
            return
        }

        if (isVaultUnlocked(userId = userId)) {
            // Get and save the key if necessary
            val userAutoUnlockKey =
                vaultSdkSource
                    .getUserEncryptionKey(userId = userId)
                    .getOrNull()
            authDiskSource.storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        } else {
            // Retrieve the key. If non-null, unlock the user
            authDiskSource.getUserAutoUnlockKey(userId = userId)?.let {
                val result = unlockVaultForUser(
                    userId = userId,
                    initUserCryptoMethod =
                    InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = it,
                    ),
                )
                if (result is VaultUnlockResult.Success) {
                    setVaultToUnlocked(userId = userId)
                }
            }
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
}
