package com.x8bit.bitwarden.data.platform.repository

import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

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
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val dispatcherManager: DispatcherManager,
) : SettingsRepository {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appLanguage: AppLanguage
        get() = settingsDiskSource.appLanguage ?: AppLanguage.DEFAULT
        set(value) {
            settingsDiskSource.appLanguage = value
        }

    override var appTheme: AppTheme by settingsDiskSource::appTheme

    override val appThemeStateFlow: StateFlow<AppTheme>
        get() = settingsDiskSource
            .appThemeFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.appTheme,
            )

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

    override val isUnlockWithPinEnabled: Boolean
        get() = activeUserId
            ?.let { authDiskSource.getEncryptedPin(userId = it) != null }
            ?: false

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

    override var isApprovePasswordlessLoginsEnabled: Boolean
        get() = activeUserId
            ?.let {
                settingsDiskSource.getApprovePasswordlessLoginsEnabled(it)
            }
            ?: false
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
                userId = userId,
                isApprovePasswordlessLoginsEnabled = value,
            )
        }
    override val isAutofillEnabledStateFlow: StateFlow<Boolean> =
        autofillEnabledManager.isAutofillEnabledStateFlow

    override var isScreenCaptureAllowed: Boolean
        get() = activeUserId?.let {
            settingsDiskSource.getScreenCaptureAllowed(it)
        } ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED
        set(value) {
            val userId = activeUserId ?: return
            settingsDiskSource.storeScreenCaptureAllowed(
                userId = userId,
                isScreenCaptureAllowed = value,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>
        get() = authDiskSource
            .userStateFlow
            .flatMapLatest { userState ->
                userState
                    ?.activeUserId
                    ?.let {
                        settingsDiskSource.getScreenCaptureAllowedFlow(userId = it)
                            .map { isAllowed -> isAllowed ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED }
                    }
                    ?: flowOf(DEFAULT_IS_SCREEN_CAPTURE_ALLOWED)
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = activeUserId
                    ?.let { settingsDiskSource.getScreenCaptureAllowed(userId = it) }
                    ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED,
            )

    override fun disableAutofill() {
        autofillManager.disableAutofillServices()

        // Manually indicate that autofill is no longer supported without needing a foreground state
        // change.
        autofillEnabledManager.isAutofillEnabled = false
    }

    @Suppress("ReturnCount")
    override suspend fun getUserFingerprint(): UserFingerprintResult {
        val userId = activeUserId
            ?: return UserFingerprintResult.Error

        return vaultSdkSource
            .getUserFingerprint(userId)
            .fold(
                onFailure = { UserFingerprintResult.Error },
                onSuccess = { UserFingerprintResult.Success(it) },
            )
    }

    override fun setDefaultsIfNecessary(userId: String) {
        // Set Vault Settings defaults
        if (!isVaultTimeoutActionSet(userId = userId)) {
            storeVaultTimeout(userId, VaultTimeout.ThirtyMinutes)
            storeVaultTimeoutAction(userId, VaultTimeoutAction.LOCK)
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

    override suspend fun setupBiometricsKey(): BiometricsKeyResult {
        val userId = activeUserId ?: return BiometricsKeyResult.Error
        biometricsEncryptionManager.setupBiometrics(userId)
        return vaultSdkSource
            .getUserEncryptionKey(userId = userId)
            .onSuccess {
                authDiskSource.storeUserBiometricUnlockKey(userId = userId, biometricsKey = it)
            }
            .fold(
                onSuccess = { BiometricsKeyResult.Success },
                onFailure = { BiometricsKeyResult.Error },
            )
    }

    override fun clearBiometricsKey() {
        val userId = activeUserId ?: return
        authDiskSource.storeUserBiometricUnlockKey(userId = userId, biometricsKey = null)
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
 * Returns the given [VaultTimeoutAction] or a default value if `null`.
 */
private fun VaultTimeoutAction?.orDefault(): VaultTimeoutAction =
    this ?: VaultTimeoutAction.LOCK
