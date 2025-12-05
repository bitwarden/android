package com.x8bit.bitwarden.data.vault.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.concurrentMapOf
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokens
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.model.AppCreationState
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.logTag
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * The number of times a user may fail to unlock before they are automatically logged out.
 */
private const val MAXIMUM_INVALID_UNLOCK_ATTEMPTS = 5

/**
 * Primary implementation [VaultLockManager].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class VaultLockManagerImpl(
    private val clock: Clock,
    private val realtimeManager: RealtimeManager,
    private val authDiskSource: AuthDiskSource,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
    private val settingsRepository: SettingsRepository,
    private val appStateManager: AppStateManager,
    private val userLogoutManager: UserLogoutManager,
    private val trustedDeviceManager: TrustedDeviceManager,
    private val kdfManager: KdfManager,
    private val pinProtectedUserKeyManager: PinProtectedUserKeyManager,
    dispatcherManager: DispatcherManager,
    context: Context,
) : VaultLockManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    /**
     * This [Map] tracks all active timeout [Job]s that are running and their associated data using
     * the user ID as the key.
     */
    private val userIdTimerJobMap: MutableMap<String, TimeoutJobData> = concurrentMapOf()

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableVaultUnlockDataStateFlow =
        MutableStateFlow<List<VaultUnlockData>>(emptyList())
    private val mutableVaultStateEventSharedFlow = bufferedMutableSharedFlow<VaultStateEvent>()

    override val vaultUnlockDataStateFlow: StateFlow<List<VaultUnlockData>>
        get() = mutableVaultUnlockDataStateFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isActiveUserUnlockingFlow: StateFlow<Boolean>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { activeUserId ->
                vaultUnlockDataStateFlow.map { vaultUnlockData ->
                    vaultUnlockData.any {
                        it.userId == activeUserId && it.status == VaultUnlockData.Status.UNLOCKING
                    }
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = mutableVaultUnlockDataStateFlow.value.any {
                    it.userId == activeUserId && it.status == VaultUnlockData.Status.UNLOCKING
                },
            )

    override val vaultStateEventFlow: Flow<VaultStateEvent>
        get() = mutableVaultStateEventSharedFlow.asSharedFlow()

    override var isFromLockFlow: Boolean = false

    init {
        observeAppCreationChanges()
        observeAppForegroundChanges()
        observeUserSwitchingChanges()
        observeVaultTimeoutChanges()
        observeUserLogoutResults()
        context.registerReceiver(
            ScreenStateBroadcastReceiver(),
            IntentFilter(Intent.ACTION_SCREEN_ON),
        )
    }

    override fun isVaultUnlocked(userId: String): Boolean =
        mutableVaultUnlockDataStateFlow.value.statusFor(userId) == VaultUnlockData.Status.UNLOCKED

    override fun isVaultUnlocking(userId: String): Boolean =
        mutableVaultUnlockDataStateFlow.value.statusFor(userId) == VaultUnlockData.Status.UNLOCKING

    override fun lockVault(userId: String, isUserInitiated: Boolean) {
        isFromLockFlow = isUserInitiated
        setVaultToLocked(userId = userId)
    }

    override fun lockVaultForCurrentUser(isUserInitiated: Boolean) {
        activeUserId?.let {
            lockVault(
                userId = it,
                isUserInitiated = isUserInitiated,
            )
        }
    }

    @Suppress("LongMethod")
    override suspend fun unlockVault(
        userId: String,
        email: String,
        kdf: Kdf,
        privateKey: String,
        signingKey: String?,
        securityState: String?,
        initUserCryptoMethod: InitUserCryptoMethod,
        organizationKeys: Map<String, String>?,
    ): VaultUnlockResult = withContext(context = NonCancellable) {
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
                            userId = userId,
                            signingKey = signingKey,
                            securityState = securityState,
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
                            VaultUnlockResult.GenericError(error = it)
                        },
                        onSuccess = { initializeCryptoResult ->
                            initializeCryptoResult
                                .toVaultUnlockResult()
                                .also {
                                    hashAndStoreMasterPassword(
                                        initUserCryptoMethod = initUserCryptoMethod,
                                        email = email,
                                        kdf = kdf,
                                        userId = userId,
                                    )
                                    if (it is VaultUnlockResult.Success) {
                                        Timber.d(
                                            "[Auth] Vault unlocked, method:  %s",
                                            initUserCryptoMethod.logTag,
                                        )
                                        clearInvalidUnlockCount(userId = userId)
                                        trustedDeviceManager
                                            .trustThisDeviceIfNecessary(userId = userId)
                                        updateKdfIfNeeded(initUserCryptoMethod)
                                        pinProtectedUserKeyManager
                                            .migratePinProtectedUserKeyIfNeeded(userId = userId)
                                        setVaultToUnlocked(userId = userId)
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
    }

    /**
     * Hashes a password and stores it as the master password hash for a given user.
     */
    private suspend fun hashAndStoreMasterPassword(
        initUserCryptoMethod: InitUserCryptoMethod,
        email: String,
        kdf: Kdf,
        userId: String,
    ) {
        (initUserCryptoMethod as? InitUserCryptoMethod.MasterPasswordUnlock)?.let {
            // Save the master password hash.
            authSdkSource
                .hashPassword(
                    email = email,
                    password = initUserCryptoMethod.password,
                    kdf = kdf,
                    purpose = HashPurpose.LOCAL_AUTHORIZATION,
                )
                .onSuccess {
                    authDiskSource.storeMasterPasswordHash(
                        userId = userId,
                        passwordHash = it,
                    )
                }
        }
    }

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
            userLogoutManager.logout(userId = userId, reason = LogoutReason.TooManyUnlockAttempts)
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
            authDiskSource.storeLastLockTimestamp(
                userId = userId,
                lastLockTimestamp = clock.instant(),
            )
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

    private fun observeAppCreationChanges() {
        var isFirstCreated = true
        appStateManager
            .appCreatedStateFlow
            .onEach { appCreationState ->
                when (appCreationState) {
                    is AppCreationState.Created -> {
                        handleOnCreated(
                            createdForAutofill = appCreationState.isAutoFill,
                            isFirstCreated = isFirstCreated,
                        )
                        isFirstCreated = false
                    }

                    AppCreationState.Destroyed -> Unit
                }
            }
            .launchIn(unconfinedScope)
    }

    private fun handleOnCreated(
        createdForAutofill: Boolean,
        isFirstCreated: Boolean,
    ) {
        val userId = activeUserId ?: return
        checkForVaultTimeout(
            userId = userId,
            checkTimeoutReason = CheckTimeoutReason.AppCreated(
                firstTimeCreation = isFirstCreated,
                createdForAutofill = createdForAutofill,
            ),
        )
    }

    private fun observeAppForegroundChanges() {
        appStateManager
            .appForegroundStateFlow
            .onEach { appForegroundState ->
                when (appForegroundState) {
                    AppForegroundState.BACKGROUNDED -> {
                        handleOnBackground()
                    }

                    AppForegroundState.FOREGROUNDED -> handleOnForeground()
                }
            }
            .launchIn(unconfinedScope)
    }

    private fun handleOnBackground() {
        val userId = activeUserId ?: return
        checkForVaultTimeout(
            userId = userId,
            checkTimeoutReason = CheckTimeoutReason.AppBackgrounded,
        )
    }

    private fun handleOnForeground() {
        val userId = activeUserId ?: return
        userIdTimerJobMap.remove(key = userId)?.job?.cancel()
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

    private fun observeUserLogoutResults() {
        userLogoutManager
            .logoutEventFlow
            .onEach {
                setVaultToLocked(it.loggedOutUserId)
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
        // Make sure to clear the now-active user's timeout job.
        userIdTimerJobMap.remove(key = currentActiveUserId)?.job?.cancel()
        // Check if the user's timeout action should be performed as we switch away.
        checkForVaultTimeout(
            userId = previousActiveUserId,
            checkTimeoutReason = CheckTimeoutReason.UserChanged,
        )
    }

    /**
     * Checks the current [VaultTimeout] for the given [userId]. If the given timeout value has
     * been exceeded, the [VaultTimeoutAction] for the given user will be performed.
     */
    private fun checkForVaultTimeout(
        userId: String,
        checkTimeoutReason: CheckTimeoutReason,
    ) {
        // Check if the user is already logged out. If this is the case no need to check timeout.
        // This is required in the case that an account has been "soft logged out" and has an
        // immediate time interval timeout. Without this check it would be automatically switch
        // the active user back to an authenticated user if one exists.
        if (isUserLoggedOut(userId = userId)) return
        val vaultTimeout = settingsRepository.getVaultTimeoutStateFlow(userId = userId).value
        val vaultTimeoutAction = settingsRepository
            .getVaultTimeoutActionStateFlow(userId = userId)
            .value

        when (vaultTimeout) {
            VaultTimeout.Never -> {
                // No action to take for Never timeout.
                return
            }

            VaultTimeout.OnAppRestart -> {
                // If this is an app restart, trigger the timeout action; otherwise ignore.
                if (checkTimeoutReason is CheckTimeoutReason.AppCreated) {
                    // We need to check the timeout action on the first time creation no matter what
                    // for all subsequent creations we should check if this is for autofill and
                    // and if it is we skip checking the timeout action.
                    if (
                        checkTimeoutReason.firstTimeCreation ||
                        !checkTimeoutReason.createdForAutofill
                    ) {
                        handleTimeoutAction(
                            userId = userId,
                            vaultTimeoutAction = vaultTimeoutAction,
                        )
                    }
                }
            }

            else -> {
                when (checkTimeoutReason) {
                    // Always preform the timeout action on app restart to ensure the user is
                    // in the correct state.
                    is CheckTimeoutReason.AppCreated -> {
                        if (checkTimeoutReason.firstTimeCreation) {
                            handleTimeoutAction(
                                userId = userId,
                                vaultTimeoutAction = vaultTimeoutAction,
                            )
                        }
                    }

                    // User no longer active or engaging with the app.
                    CheckTimeoutReason.AppBackgrounded,
                    CheckTimeoutReason.UserChanged,
                        -> {
                        handleTimeoutActionWithDelay(
                            userId = userId,
                            vaultTimeoutAction = vaultTimeoutAction,
                            delayMs = vaultTimeout
                                .vaultTimeoutInMinutes
                                ?.minutes
                                ?.inWholeMilliseconds
                                ?: 0L,
                        )
                    }
                }
            }
        }
    }

    /**
     * Performs the [VaultTimeoutAction] for the given [userId] after the [delayMs] has passed.
     *
     * @see handleTimeoutAction
     */
    private fun handleTimeoutActionWithDelay(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction,
        delayMs: Long,
    ) {
        userIdTimerJobMap.remove(key = userId)?.job?.cancel()
        userIdTimerJobMap[userId] = TimeoutJobData(
            job = unconfinedScope.launch {
                delay(timeMillis = delayMs)
                userIdTimerJobMap.remove(key = userId)
                handleTimeoutAction(userId = userId, vaultTimeoutAction = vaultTimeoutAction)
            },
            vaultTimeoutAction = vaultTimeoutAction,
            startTimeMs = realtimeManager.elapsedRealtimeMs,
            durationMs = delayMs,
        )
    }

    /**
     * Performs a lock or soft-logout operation for the given [userId] based on the provided
     * [VaultTimeoutAction].
     */
    private fun handleTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction,
    ) {
        when (vaultTimeoutAction) {
            VaultTimeoutAction.LOCK -> {
                setVaultToLocked(userId = userId)
            }

            VaultTimeoutAction.LOGOUT -> {
                userLogoutManager.softLogout(userId = userId, reason = LogoutReason.Timeout)
            }
        }
    }

    private suspend fun unlockVaultForUser(
        userId: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val account = authDiskSource.userState?.accounts?.get(userId)
            ?: return VaultUnlockResult.InvalidStateError(error = NoActiveUserException())
        val accountKeys = authDiskSource.getAccountKeys(userId = userId)
        val privateKey = accountKeys?.publicKeyEncryptionKeyPair?.wrappedPrivateKey
            ?: authDiskSource.getPrivateKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                error = MissingPropertyException("Private key"),
            )
        val signingKey = accountKeys?.signatureKeyPair?.wrappedSigningKey
        val securityState = accountKeys?.securityState?.securityState
        val organizationKeys = authDiskSource.getOrganizationKeys(userId = userId)
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

    private fun isUserLoggedOut(userId: String): Boolean {
        val accounts = authDiskSource.userAccountTokens
        return (accounts.find { it.userId == userId }?.isLoggedIn) == false
    }

    private suspend fun updateKdfIfNeeded(initUserCryptoMethod: InitUserCryptoMethod) {
        val password = when (initUserCryptoMethod) {
            is InitUserCryptoMethod.MasterPasswordUnlock -> initUserCryptoMethod.password
            is InitUserCryptoMethod.AuthRequest,
            is InitUserCryptoMethod.DecryptedKey,
            is InitUserCryptoMethod.DeviceKey,
            is InitUserCryptoMethod.KeyConnector,
            is InitUserCryptoMethod.Password,
            is InitUserCryptoMethod.Pin,
            is InitUserCryptoMethod.PinEnvelope,
                -> return
        }

        kdfManager
            .updateKdfToMinimumsIfNeeded(
                password = password,
            )
            .also { result ->
                if (result is UpdateKdfMinimumsResult.Error) {
                    Timber.e(result.error, message = "Failed to silent update KDF settings.")
                }
            }
    }

    /**
     * A custom [BroadcastReceiver] that listens for when the screen is powered on and restarts the
     * vault timeout jobs to ensure they complete at the correct time.
     *
     * This is necessary because the [delay] function in a coroutine will not keep accurate time
     * when the screen is off. We do not cancel the job when the screen is off, this allows the
     * job to complete as-soon-as possible if the screen is powered off for an extended period.
     */
    private inner class ScreenStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            userIdTimerJobMap.map { (userId, data) ->
                val durationSoFarMs = (realtimeManager.elapsedRealtimeMs - data.startTimeMs)
                    .coerceAtLeast(minimumValue = 0L)
                handleTimeoutActionWithDelay(
                    userId = userId,
                    vaultTimeoutAction = data.vaultTimeoutAction,
                    delayMs = data.durationMs - durationSoFarMs,
                )
            }
        }
    }

    /**
     * A wrapper class containing all relevant data concerning a timeout action [Job].
     */
    private data class TimeoutJobData(
        val job: Job,
        val vaultTimeoutAction: VaultTimeoutAction,
        val startTimeMs: Long,
        val durationMs: Long,
    )

    /**
     * Helper sealed class which denotes the reason to check the vault timeout.
     */
    private sealed class CheckTimeoutReason {
        /**
         * Indicates the app has been backgrounded but is still running.
         */
        data object AppBackgrounded : CheckTimeoutReason()

        /**
         * Indicates the app has entered a Created state.
         *
         * @param firstTimeCreation if this is the first time the process is being created.
         * @param createdForAutofill if the the creation event is due to an activity being launched
         * for autofill.
         */
        data class AppCreated(
            val firstTimeCreation: Boolean,
            val createdForAutofill: Boolean,
        ) : CheckTimeoutReason()

        /**
         * Indicates that the current user has changed.
         */
        data object UserChanged : CheckTimeoutReason()
    }
}
