package com.x8bit.bitwarden.data.platform.repository

import android.view.autofill.AutofillManager
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderManager
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.crypto.Cipher

private val DEFAULT_IS_SCREEN_CAPTURE_ALLOWED = BuildConfig.DEBUG

/**
 * Primary implementation of [SettingsRepository].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class SettingsRepositoryImpl(
    private val autofillManager: AutofillManager,
    private val autofillEnabledManager: AutofillEnabledManager,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    flightRecorderManager: FlightRecorderManager,
    accessibilityEnabledManager: AccessibilityEnabledManager,
    policyManager: PolicyManager,
    dispatcherManager: DispatcherManager,
) : SettingsRepository,
    FlightRecorderManager by flightRecorderManager {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appLanguage: AppLanguage
        get() = settingsDiskSource.appLanguage ?: AppLanguage.DEFAULT
        set(value) {
            settingsDiskSource.appLanguage = value
        }

    override val appLanguageStateFlow: StateFlow<AppLanguage>
        get() = settingsDiskSource
            .appLanguageFlow
            .map { it ?: AppLanguage.DEFAULT }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = appLanguage,
            )

    override var appTheme: AppTheme by settingsDiskSource::appTheme

    override val appThemeStateFlow: StateFlow<AppTheme>
        get() = settingsDiskSource
            .appThemeFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.appTheme,
            )

    override var isDynamicColorsEnabled: Boolean
        get() = settingsDiskSource.isDynamicColorsEnabled ?: false
        set(value) {
            settingsDiskSource.isDynamicColorsEnabled = value
        }

    override val isDynamicColorsEnabledFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .isDynamicColorsEnabledFlow
            .map { it ?: false }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.isDynamicColorsEnabled ?: false,
            )

    override var initialAutofillDialogShown: Boolean
        get() = settingsDiskSource.initialAutofillDialogShown ?: false
        set(value) {
            settingsDiskSource.initialAutofillDialogShown = value
        }

    override var vaultLastSync: Instant?
        get() = vaultLastSyncStateFlow.value
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = value)
        }

    override val vaultLastSyncStateFlow: StateFlow<Instant?>
        get() = activeUserId
            ?.let {
                settingsDiskSource
                    .getLastSyncTimeFlow(userId = it)
                    .stateIn(
                        scope = unconfinedScope,
                        started = SharingStarted.Eagerly,
                        initialValue = settingsDiskSource.getLastSyncTime(userId = it),
                    )
            }
            ?: MutableStateFlow(value = null)

    override var isAuthenticatorSyncEnabled: Boolean
        // Authenticator sync is enabled if there is an authenticator sync unlock key for
        // the current active user:
        get() = activeUserId
            ?.let { authDiskSource.getAuthenticatorSyncUnlockKey(userId = it) != null }
            ?: false
        set(value) {
            val userId = activeUserId ?: return
            // When turning off authenticator sync, set authenticator sync unlock key to
            // null for the current active user:
            if (!value) {
                authDiskSource.storeAuthenticatorSyncUnlockKey(
                    userId = userId,
                    authenticatorSyncUnlockKey = null,
                )
                return
            }
            // When turning on authenticator sync, get a user encryption key from the vault SDK
            // and store it as a authenticator sync unlock key. Also, generate a
            // symmetric sync key if needed:
            generateSymmetricSyncKeyIfNecessary()
            unconfinedScope.launch {
                vaultSdkSource
                    .getUserEncryptionKey(userId = userId)
                    .getOrNull()
                    ?.let {
                        authDiskSource.storeAuthenticatorSyncUnlockKey(
                            userId = userId,
                            authenticatorSyncUnlockKey = it,
                        )
                    }
            }
        }

    override var isIconLoadingDisabled: Boolean
        get() = settingsDiskSource.isIconLoadingDisabled ?: false
        set(value) {
            settingsDiskSource.isIconLoadingDisabled = value
        }

    override val isIconLoadingDisabledFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .isIconLoadingDisabledFlow
            .map { it ?: false }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .isIconLoadingDisabled
                    ?: false,
            )
    override var clearClipboardFrequency: ClearClipboardFrequency
        get() = activeUserId
            ?.let { userId ->
                settingsDiskSource
                    .getClearClipboardFrequencySeconds(userId)
                    .toClearClipboardFrequency()
            }
            ?: ClearClipboardFrequency.NEVER
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeClearClipboardFrequencySeconds(userId, value.frequencySeconds)
        }

    override var isCrashLoggingEnabled: Boolean
        get() = settingsDiskSource.isCrashLoggingEnabled ?: true
        set(value) {
            settingsDiskSource.isCrashLoggingEnabled = value
        }

    override val isCrashLoggingEnabledFlow: Flow<Boolean>
        get() = settingsDiskSource
            .isCrashLoggingEnabledFlow
            .map { it ?: isCrashLoggingEnabled }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = isCrashLoggingEnabled,
            )

    override var hasUserLoggedInOrCreatedAccount: Boolean
        get() = settingsDiskSource.hasUserLoggedInOrCreatedAccount ?: false
        set(value) {
            settingsDiskSource.hasUserLoggedInOrCreatedAccount = value
        }

    override val hasUserLoggedInOrCreatedAccountFlow: Flow<Boolean>
        get() = settingsDiskSource
            .hasUserLoggedInOrCreatedAccountFlow
            .map { it ?: hasUserLoggedInOrCreatedAccount }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = hasUserLoggedInOrCreatedAccount,
            )

    override var vaultTimeout: VaultTimeout
        get() = activeUserId
            ?.let {
                getVaultTimeoutStateFlow(userId = it).value
            }
            ?: VaultTimeout.Never
        set(value) {
            val userId = activeUserId ?: return
            storeVaultTimeout(
                userId = userId,
                vaultTimeout = value,
            )
        }

    override var vaultTimeoutAction: VaultTimeoutAction
        get() = activeUserId
            ?.let {
                getVaultTimeoutActionStateFlow(userId = it).value
            }
            .orDefault()
        set(value) {
            val userId = activeUserId ?: return
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = value,
            )
        }

    override var defaultUriMatchType: UriMatchType
        get() = activeUserId
            ?.let {
                settingsDiskSource.getDefaultUriMatchType(userId = it)
            }
            ?: UriMatchType.DOMAIN
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeDefaultUriMatchType(
                userId = userId,
                uriMatchType = value,
            )
        }

    override val isUnlockWithBiometricsEnabled: Boolean
        get() = activeUserId
            ?.let { authDiskSource.getUserBiometricUnlockKey(userId = it) != null }
            ?: false

    override val isUnlockWithBiometricsEnabledFlow: Flow<Boolean>
        get() = activeUserId
            ?.let { userId ->
                authDiskSource
                    .getUserBiometicUnlockKeyFlow(userId)
                    .map { it != null }
            }
            ?: flowOf(false)

    override val isUnlockWithPinEnabled: Boolean
        get() = activeUserId
            ?.let { authDiskSource.getEncryptedPin(userId = it) != null }
            ?: false

    override val isUnlockWithPinEnabledFlow: Flow<Boolean>
        get() = activeUserId
            ?.let { userId ->
                authDiskSource
                    .getPinProtectedUserKeyFlow(userId)
                    .map { it != null }
            }
            ?: flowOf(false)

    override var isInlineAutofillEnabled: Boolean
        get() = activeUserId
            ?.let { settingsDiskSource.getInlineAutofillEnabled(userId = it) }
            ?: true
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeInlineAutofillEnabled(
                userId = userId,
                isInlineAutofillEnabled = value,
            )
        }

    override var isAutoCopyTotpDisabled: Boolean
        get() = activeUserId
            ?.let { settingsDiskSource.getAutoCopyTotpDisabled(userId = it) }
            ?: false
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeAutoCopyTotpDisabled(
                userId = userId,
                isAutomaticallyCopyTotpDisabled = value,
            )
        }

    override var isAutofillSavePromptDisabled: Boolean
        get() = activeUserId
            ?.let { settingsDiskSource.getAutofillSavePromptDisabled(userId = it) }
            ?: false
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeAutofillSavePromptDisabled(
                userId = userId,
                isAutofillSavePromptDisabled = value,
            )
        }

    override var blockedAutofillUris: List<String>
        get() = activeUserId
            ?.let { settingsDiskSource.getBlockedAutofillUris(userId = it) }
            ?: emptyList()
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeBlockedAutofillUris(
                userId = userId,
                blockedAutofillUris = value,
            )
        }

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean> =
        accessibilityEnabledManager.isAccessibilityEnabledStateFlow

    override val isAutofillEnabledStateFlow: StateFlow<Boolean> =
        autofillEnabledManager.isAutofillEnabledStateFlow

    override var isScreenCaptureAllowed: Boolean
        get() = settingsDiskSource.screenCaptureAllowed ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED
        set(value) {
            settingsDiskSource.screenCaptureAllowed = value
        }

    override val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .screenCaptureAllowedFlow
            .map { isAllowed -> isAllowed ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = isScreenCaptureAllowed,
            )

    init {
        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
            .onEach { updateVaultUnlockSettingsIfNecessary(it) }
            .launchIn(unconfinedScope)
    }

    override fun disableAutofill() {
        autofillManager.disableAutofillServices()

        // Manually indicate that autofill is no longer supported without needing a foreground state
        // change.
        autofillEnabledManager.isAutofillEnabled = false
    }

    override suspend fun getUserFingerprint(): UserFingerprintResult {
        val userId = activeUserId
            ?: return UserFingerprintResult.Error(error = NoActiveUserException())

        return vaultSdkSource
            .getUserFingerprint(userId)
            .fold(
                onFailure = { UserFingerprintResult.Error(error = it) },
                onSuccess = { UserFingerprintResult.Success(it) },
            )
    }

    override fun setDefaultsIfNecessary(userId: String) {
        // Set Vault Settings defaults
        val hasMasterPassword = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?.userDecryptionOptions
            ?.hasMasterPassword != false
        val timeoutAction = settingsDiskSource.getVaultTimeoutAction(userId = userId)
        val hasPin = authDiskSource.getPinProtectedUserKey(userId = userId) != null
        val hasBiometrics = authDiskSource.getUserBiometricUnlockKey(userId = userId) != null
        // The timeout action cannot be "lock" if you do not have master password, pin, or
        // biometrics unlock enabled.
        val hasInvalidTimeoutAction = timeoutAction == VaultTimeoutAction.LOCK &&
            !hasPin &&
            !hasBiometrics &&
            !hasMasterPassword
        if (!isVaultTimeoutActionSet(userId = userId) || hasInvalidTimeoutAction) {
            storeVaultTimeout(userId, VaultTimeout.FifteenMinutes)
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = if (!hasMasterPassword) {
                    // Always logout by default when there is no master password
                    VaultTimeoutAction.LOGOUT
                } else {
                    VaultTimeoutAction.LOCK
                },
            )
        }
    }

    override fun getVaultTimeoutStateFlow(userId: String): StateFlow<VaultTimeout> =
        settingsDiskSource
            .getVaultTimeoutInMinutesFlow(userId = userId)
            .map { it.toVaultTimeout() }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getVaultTimeoutInMinutes(userId = userId)
                    .toVaultTimeout(),
            )

    override fun storeVaultTimeout(userId: String, vaultTimeout: VaultTimeout) {
        settingsDiskSource.storeVaultTimeoutInMinutes(
            userId = userId,
            vaultTimeoutInMinutes = vaultTimeout.vaultTimeoutInMinutes,
        )
    }

    override fun getVaultTimeoutActionStateFlow(
        userId: String,
    ): StateFlow<VaultTimeoutAction> =
        settingsDiskSource
            .getVaultTimeoutActionFlow(userId = userId)
            .map { it.orDefault() }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getVaultTimeoutAction(userId = userId)
                    .orDefault(),
            )

    override fun isVaultTimeoutActionSet(
        userId: String,
    ): Boolean = settingsDiskSource.getVaultTimeoutAction(userId = userId) != null

    override fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    ) {
        settingsDiskSource.storeVaultTimeoutAction(
            userId = userId,
            vaultTimeoutAction = vaultTimeoutAction,
        )
    }

    override fun getPullToRefreshEnabledFlow(): StateFlow<Boolean> {
        val userId = activeUserId ?: return MutableStateFlow(false)
        return settingsDiskSource
            .getPullToRefreshEnabledFlow(userId = userId)
            .map { it ?: false }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getPullToRefreshEnabled(userId = userId)
                    ?: false,
            )
    }

    override fun storePullToRefreshEnabled(isPullToRefreshEnabled: Boolean) {
        activeUserId?.let {
            settingsDiskSource.storePullToRefreshEnabled(
                userId = it,
                isPullToRefreshEnabled = isPullToRefreshEnabled,
            )
        }
    }

    override suspend fun setupBiometricsKey(cipher: Cipher): BiometricsKeyResult {
        val userId = activeUserId
            ?: return BiometricsKeyResult.Error(error = NoActiveUserException())
        return vaultSdkSource
            .getUserEncryptionKey(userId = userId)
            .onSuccess { biometricsKey ->
                authDiskSource.storeUserBiometricUnlockKey(
                    userId = userId,
                    biometricsKey = cipher
                        .doFinal(biometricsKey.encodeToByteArray())
                        .toString(Charsets.ISO_8859_1),
                )
                authDiskSource.storeUserBiometricInitVector(userId = userId, iv = cipher.iv)
            }
            .fold(
                onSuccess = { BiometricsKeyResult.Success },
                onFailure = { BiometricsKeyResult.Error(error = it) },
            )
    }

    override fun storeUnlockPin(
        pin: String,
        shouldRequireMasterPasswordOnRestart: Boolean,
    ) {
        val userId = activeUserId ?: return
        unconfinedScope.launch {
            vaultSdkSource
                .derivePinKey(
                    userId = userId,
                    pin = pin,
                )
                .fold(
                    onSuccess = { derivePinKeyResponse ->
                        authDiskSource.apply {
                            storeEncryptedPin(
                                userId = userId,
                                encryptedPin = derivePinKeyResponse.encryptedPin,
                            )
                            storePinProtectedUserKey(
                                userId = userId,
                                pinProtectedUserKey = derivePinKeyResponse.pinProtectedUserKey,
                                inMemoryOnly = shouldRequireMasterPasswordOnRestart,
                            )
                        }
                    },
                    onFailure = {
                        // PIN derivation should only fail when the user's vault is locked. This
                        // should not be a concern when this method is actually called so we should
                        // be able to safely ignore this.
                    },
                )
        }
    }

    override fun clearUnlockPin() {
        val userId = activeUserId ?: return
        authDiskSource.apply {
            storeEncryptedPin(
                userId = userId,
                encryptedPin = null,
            )
            authDiskSource.storePinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = null,
            )
        }
    }

    override fun getUserHasLoggedInValue(userId: String): Boolean =
        settingsDiskSource.getUserHasSignedInPreviously(userId)

    override fun storeUserHasLoggedInValue(userId: String) {
        settingsDiskSource.storeUseHasLoggedInPreviously(userId)
    }

    override fun isVaultRegisteredForExport(userId: String): Boolean {
        return settingsDiskSource.getVaultRegisteredForExport(userId) == true
    }

    override fun storeVaultRegisteredForExport(userId: String, isRegistered: Boolean) {
        settingsDiskSource.storeVaultRegisteredForExport(userId, isRegistered)
    }

    override fun getVaultRegisteredForExportFlow(userId: String): StateFlow<Boolean> {
        return settingsDiskSource
            .getVaultRegisteredForExportFlow(userId)
            .map { it ?: false }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getVaultRegisteredForExport(userId)
                    ?: false,
            )
    }

    /**
     * If there isn't already one generated, generate a symmetric sync key that would be used
     * for communicating via IPC.
     */
    private fun generateSymmetricSyncKeyIfNecessary() {
        // If there is already an authenticator sync symmetric key, do nothing:
        if (authDiskSource.authenticatorSyncSymmetricKey != null) {
            return
        }
        // Otherwise, generate and store a key:
        val secretKey = generateSecretKey().getOrNull() ?: return
        authDiskSource.authenticatorSyncSymmetricKey = secretKey.encoded
    }

    /**
     * Check the parameters of the vault unlock policy against the user's
     * settings to determine whether to update the user's settings.
     */
    private fun updateVaultUnlockSettingsIfNecessary(
        policies: List<SyncResponseJson.Policy>,
    ) {
        // The vault timeout policy can only be implemented in organizations that have
        // the single organization policy, meaning that if this is enabled, the user is
        // only in one organization and hence there is only one result in the list.
        val vaultUnlockPolicy = policies
            .firstOrNull()
            ?.policyInformation as? PolicyInformation.VaultTimeout
            ?: return

        // Adjust the user's timeout or method if necessary to meet the policy requirements.
        vaultUnlockPolicy.minutes?.let { maxMinutes ->
            if ((vaultTimeout.vaultTimeoutInMinutes ?: Int.MAX_VALUE) > maxMinutes) {
                vaultTimeout = VaultTimeout.Custom(maxMinutes)
            }
        }
        vaultUnlockPolicy.action?.let {
            vaultTimeoutAction = if (it == "lock") {
                VaultTimeoutAction.LOCK
            } else {
                VaultTimeoutAction.LOGOUT
            }
        }
    }
}

/**
 * Converts a stored [Int] representing a vault timeout in minutes to a [VaultTimeout].
 */
private fun Int?.toVaultTimeout(): VaultTimeout =
    when (this) {
        VaultTimeout.Immediately.vaultTimeoutInMinutes -> VaultTimeout.Immediately
        VaultTimeout.OneMinute.vaultTimeoutInMinutes -> VaultTimeout.OneMinute
        VaultTimeout.FiveMinutes.vaultTimeoutInMinutes -> VaultTimeout.FiveMinutes
        VaultTimeout.FifteenMinutes.vaultTimeoutInMinutes -> VaultTimeout.FifteenMinutes
        VaultTimeout.ThirtyMinutes.vaultTimeoutInMinutes -> VaultTimeout.ThirtyMinutes
        VaultTimeout.OneHour.vaultTimeoutInMinutes -> VaultTimeout.OneHour
        VaultTimeout.FourHours.vaultTimeoutInMinutes -> VaultTimeout.FourHours
        VaultTimeout.OnAppRestart.vaultTimeoutInMinutes -> VaultTimeout.OnAppRestart
        null -> VaultTimeout.Never
        else -> VaultTimeout.Custom(vaultTimeoutInMinutes = this)
    }

/**
 * Converts the given Int into a [ClearClipboardFrequency] item.
 */
private fun Int?.toClearClipboardFrequency(): ClearClipboardFrequency =
    ClearClipboardFrequency.entries.firstOrNull { it.frequencySeconds == this }
        ?: ClearClipboardFrequency.NEVER

/**
 * Returns the given [VaultTimeoutAction] or a default value if `null`.
 */
private fun VaultTimeoutAction?.orDefault(): VaultTimeoutAction =
    this ?: VaultTimeoutAction.LOCK
