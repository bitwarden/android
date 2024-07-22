package com.x8bit.bitwarden.data.vault.manager

import android.os.SystemClock
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokens
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import kotlinx.coroutines.launch

private const val SECONDS_PER_MINUTE = 60
private const val MILLISECONDS_PER_SECOND = 1000

/**
 * The number of times a user may fail to unlock before they are automatically logged out.
 */
private const val MAXIMUM_INVALID_UNLOCK_ATTEMPTS = 5

/**
 * Primary implementation [VaultLockManager].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class VaultLockManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
    private val settingsRepository: SettingsRepository,
    private val appForegroundManager: AppForegroundManager,
    private val userLogoutManager: UserLogoutManager,
    private val trustedDeviceManager: TrustedDeviceManager,
    dispatcherManager: DispatcherManager,
    private val elapsedRealtimeMillisProvider: () -> Long = { SystemClock.elapsedRealtime() },
) : VaultLockManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId
    private val userIds: Set<String> get() = authDiskSource.userState?.accounts?.keys.orEmpty()

    private val mutableVaultUnlockDataStateFlow =
        MutableStateFlow<List<VaultUnlockData>>(emptyList())
    private val mutableVaultStateEventSharedFlow = bufferedMutableSharedFlow<VaultStateEvent>()

    override val vaultUnlockDataStateFlow: StateFlow<List<VaultUnlockData>>
        get() = mutableVaultUnlockDataStateFlow.asStateFlow()

    override val vaultStateEventFlow: Flow<VaultStateEvent>
        get() = mutableVaultStateEventSharedFlow.asSharedFlow()

    init {
        observeAppForegroundChanges()
        observeUserSwitchingChanges()
        observeVaultTimeoutChanges()
    }

    override fun isVaultUnlocked(userId: String): Boolean =
        mutableVaultUnlockDataStateFlow.value.statusFor(userId) == VaultUnlockData.Status.UNLOCKED

    override fun isVaultUnlocking(userId: String): Boolean =
        mutableVaultUnlockDataStateFlow.value.statusFor(userId) == VaultUnlockData.Status.UNLOCKING

    override fun lockVault(userId: String) {
        setVaultToLocked(userId = userId)
    }

    override fun lockVaultForCurrentUser() {
        activeUserId?.let {
            lockVault(it)
        }
    }

    @Suppress("LongMethod")
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
                        onFailure = {
                            incrementInvalidUnlockCount(userId = userId)
                            VaultUnlockResult.GenericError
                        },
                        onSuccess = { initializeCryptoResult ->
                            initializeCryptoResult
                                .toVaultUnlockResult()
                                .also {
                                    if (initUserCryptoMethod is InitUserCryptoMethod.Password) {
                                        // Save the master password hash.
                                        authSdkSource
                                            .hashPassword(
                                                email = email,
                                                password = initUserCryptoMethod.password,
                                                kdf = kdf,
                                                purpose = HashPurpose.LOCAL_AUTHORIZATION,
                                            )
                                            .onSuccess { passwordHash ->
                                                authDiskSource.storeMasterPasswordHash(
                                                    userId = userId,
                                                    passwordHash = passwordHash,
                                                )
                                            }
                                    }
                                }
                                .also {
                                    if (it is VaultUnlockResult.Success) {
                                        clearInvalidUnlockCount(userId = userId)
                                        setVaultToUnlocked(userId = userId)
                                        trustedDeviceManager.trustThisDeviceIfNecessary(
                                            userId = userId,
                                        )
                                    } else {
                                        incrementInvalidUnlockCount(userId = userId)
                                    }
                                }
                        },
                    ),
            )
        }
            .onCompletion { setVaultToNotUnlocking(userId = userId) }
            .first()

    override suspend fun waitUntilUnlocked(userId: String) {
        vaultUnlockDataStateFlow
            .map { vaultUnlockDataList ->
                // Get the list of currently-unlocked vaults and map them to user IDs.
                vaultUnlockDataList
                    .filter { it.status == VaultUnlockData.Status.UNLOCKED }
                    .map { it.userId }
            }
            .first { unlockedUserIds -> userId in unlockedUserIds }
    }

    override suspend fun syncVaultState(userId: String) {
        // There is no proper way to query if the vault is actually unlocked or not but we can
        // attempt to retrieve the user encryption key. If it fails, then the vault is locked and
        // if it succeeds, then the vault is unlocked.
        vaultSdkSource
            .getUserEncryptionKey(userId = userId)
            .fold(
                onFailure = { setVaultToLocked(userId = userId) },
                onSuccess = { setVaultToUnlocked(userId = userId) },
            )
    }

    /**
     * Increments the stored invalid unlock count for the given [userId] and automatically logs out
     * if this new value is greater than [MAXIMUM_INVALID_UNLOCK_ATTEMPTS].
     */
    private fun incrementInvalidUnlockCount(userId: String) {
        val previousInvalidUnlockAttempts =
            authDiskSource.getInvalidUnlockAttempts(userId = userId) ?: 0
        val invalidUnlockAttempts = previousInvalidUnlockAttempts + 1
        authDiskSource.storeInvalidUnlockAttempts(
            userId = userId,
            invalidUnlockAttempts = invalidUnlockAttempts,
        )

        if (invalidUnlockAttempts >= MAXIMUM_INVALID_UNLOCK_ATTEMPTS) {
            userLogoutManager.logout(userId)
        }
    }

    private fun clearInvalidUnlockCount(userId: String) {
        authDiskSource.storeInvalidUnlockAttempts(
            userId = userId,
            invalidUnlockAttempts = null,
        )
    }

    private fun setVaultToUnlocked(userId: String) {
        val wasVaultUnlocked = isVaultUnlocked(userId = userId)
        mutableVaultUnlockDataStateFlow.update {
            it.update(userId, VaultUnlockData.Status.UNLOCKED)
        }
        // If we are unlocking an account with a timeout of Never, we should make sure to store the
        // auto-unlock key.
        storeUserAutoUnlockKeyIfNecessary(userId = userId)
        if (!wasVaultUnlocked) {
            mutableVaultStateEventSharedFlow.tryEmit(VaultStateEvent.Unlocked(userId = userId))
        }
    }

    private fun setVaultToLocked(userId: String) {
        val wasVaultLocked = !isVaultUnlocked(userId = userId) && !isVaultUnlocking(userId = userId)
        vaultSdkSource.clearCrypto(userId = userId)
        mutableVaultUnlockDataStateFlow.update {
            it.update(userId, null)
        }
        authDiskSource.storeUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = null,
        )
        if (!wasVaultLocked) {
            mutableVaultStateEventSharedFlow.tryEmit(VaultStateEvent.Locked(userId = userId))
        }
    }

    private fun setVaultToUnlocking(userId: String) {
        mutableVaultUnlockDataStateFlow.update {
            it.update(userId, VaultUnlockData.Status.UNLOCKING)
        }
    }

    private fun setVaultToNotUnlocking(userId: String) {
        val status = mutableVaultUnlockDataStateFlow.value.statusFor(userId)
        if (status != VaultUnlockData.Status.UNLOCKING) return
        mutableVaultUnlockDataStateFlow.update {
            it.update(userId, null)
        }
    }

    private fun storeUserAutoUnlockKeyIfNecessary(userId: String) {
        val vaultTimeout = settingsRepository.getVaultTimeoutStateFlow(userId = userId).value
        if (vaultTimeout == VaultTimeout.Never &&
            authDiskSource.getUserAutoUnlockKey(userId = userId) == null
        ) {
            unconfinedScope.launch {
                vaultSdkSource
                    .getUserEncryptionKey(userId = userId)
                    .getOrNull()
                    ?.let {
                        authDiskSource.storeUserAutoUnlockKey(
                            userId = userId,
                            userAutoUnlockKey = it,
                        )
                    }
            }
        }
    }

    private fun observeAppForegroundChanges() {
        var isFirstForeground = true

        appForegroundManager
            .appForegroundStateFlow
            .onEach { appForegroundState ->
                when (appForegroundState) {
                    AppForegroundState.BACKGROUNDED -> {
                        activeUserId?.let { updateLastActiveTime(userId = it) }
                    }

                    AppForegroundState.FOREGROUNDED -> {
                        userIds.forEach { userId ->
                            // If first foreground, clear the elapsed values so the timeout action
                            // is always performed.
                            if (isFirstForeground) {
                                authDiskSource.storeLastActiveTimeMillis(
                                    userId = userId,
                                    lastActiveTimeMillis = null,
                                )
                            }
                            checkForVaultTimeout(
                                userId = userId,
                                isAppRestart = isFirstForeground,
                            )
                        }
                        isFirstForeground = false
                    }
                }
            }
            .launchIn(unconfinedScope)
    }

    private fun observeUserSwitchingChanges() {
        authDiskSource
            .userSwitchingChangesFlow
            .onEach { userSwitchingData ->
                val previousActiveUserId = userSwitchingData.previousActiveUserId
                val currentActiveUserId = userSwitchingData.currentActiveUserId
                if (previousActiveUserId != null && currentActiveUserId != null) {
                    handleUserSwitch(
                        previousActiveUserId = previousActiveUserId,
                        currentActiveUserId = currentActiveUserId,
                    )
                }
            }
            .launchIn(unconfinedScope)
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
            val userAutoUnlockKey = vaultSdkSource
                .getUserEncryptionKey(userId = userId)
                .getOrNull()
            authDiskSource.storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        } else {
            // Retrieve the key. If non-null, unlock the user
            authDiskSource.getUserAutoUnlockKey(userId = userId)?.let {
                unlockVaultForUser(
                    userId = userId,
                    initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = it,
                    ),
                )
            }
        }
    }

    /**
     * Handles any vault timeout actions that may need to be performed for the given
     * [previousActiveUserId] and [currentActiveUserId] during an account switch.
     */
    private fun handleUserSwitch(
        previousActiveUserId: String,
        currentActiveUserId: String,
    ) {
        // Check if the user's timeout action should be performed as we switch away.
        checkForVaultTimeout(userId = previousActiveUserId)

        // Set the last active time for the previous user.
        updateLastActiveTime(userId = previousActiveUserId)

        // Check if the vault timeout action should be performed for the current user
        checkForVaultTimeout(userId = currentActiveUserId)

        // Set the last active time for the current user.
        updateLastActiveTime(userId = currentActiveUserId)
    }

    /**
     * Checks the current [VaultTimeout] for the given [userId]. If the given timeout value has
     * been exceeded, the [VaultTimeoutAction] for the given user will be performed.
     */
    private fun checkForVaultTimeout(
        userId: String,
        isAppRestart: Boolean = false,
    ) {
        val accounts = authDiskSource.userAccountTokens
        /**
         * Check if the user is already logged out. If this is the case no need to check timeout.
         * This is required in the case that an account has been "soft logged out" and has an
         * immediate time interval time out. Without this check it would be automatically switch
         * the active user back to an authenticated user if one exists.
         */
        if ((accounts.find { it.userId == userId }?.isLoggedIn) == false) {
            return
        }

        val currentTimeMillis = elapsedRealtimeMillisProvider()
        val lastActiveTimeMillis = authDiskSource.getLastActiveTimeMillis(userId = userId) ?: 0
        val vaultTimeout = settingsRepository.getVaultTimeoutStateFlow(userId = userId).value
        val vaultTimeoutAction = settingsRepository
            .getVaultTimeoutActionStateFlow(userId = userId)
            .value

        val vaultTimeoutInMinutes = when (vaultTimeout) {
            VaultTimeout.Never -> {
                // No action to take for Never timeout.
                return
            }

            VaultTimeout.OnAppRestart -> {
                // If this is an app restart, trigger the timeout action; otherwise ignore.
                if (isAppRestart) 0 else return
            }

            else -> vaultTimeout.vaultTimeoutInMinutes ?: return
        }
        val vaultTimeoutInMillis = vaultTimeoutInMinutes *
            SECONDS_PER_MINUTE *
            MILLISECONDS_PER_SECOND
        if (currentTimeMillis - lastActiveTimeMillis >= vaultTimeoutInMillis) {
            // Perform lock / logout!
            when (vaultTimeoutAction) {
                VaultTimeoutAction.LOCK -> {
                    setVaultToLocked(userId = userId)
                }

                VaultTimeoutAction.LOGOUT -> {
                    setVaultToLocked(userId = userId)
                    userLogoutManager.softLogout(userId = userId)
                }
            }
        }
    }

    /**
     * Sets the "last active time" for the given [userId] to the current time.
     */
    private fun updateLastActiveTime(userId: String) {
        val elapsedRealtimeMillis = elapsedRealtimeMillisProvider()
        authDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = elapsedRealtimeMillis,
        )
    }

    private suspend fun unlockVaultForUser(
        userId: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val account = authDiskSource.userState?.accounts?.get(userId)
            ?: return VaultUnlockResult.InvalidStateError
        val privateKey = authDiskSource.getPrivateKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError
        val organizationKeys = authDiskSource.getOrganizationKeys(userId = userId)
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
