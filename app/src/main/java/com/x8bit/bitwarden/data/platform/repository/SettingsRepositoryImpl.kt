package com.x8bit.bitwarden.data.platform.repository

import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Primary implementation of [SettingsRepository].
 */
@Suppress("TooManyFunctions")
class SettingsRepositoryImpl(
    private val autofillManager: AutofillManager,
    private val appForegroundManager: AppForegroundManager,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val dispatcherManager: DispatcherManager,
) : SettingsRepository {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManager.isEnabled &&
            autofillManager.hasEnabledAutofillServices() &&
            autofillManager.isAutofillSupported

    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(isAutofillEnabledAndSupported)

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
        mutableIsAutofillEnabledStateFlow.asStateFlow()

    init {
        observeAutofillEnabledChanges()
    }

    override fun disableAutofill() {
        autofillManager.disableAutofillServices()

        // Manually indicate that autofill is no longer supported without needing a foreground state
        // change.
        mutableIsAutofillEnabledStateFlow.value = false
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

    private fun observeAutofillEnabledChanges() {
        appForegroundManager
            .appForegroundStateFlow
            .onEach {
                mutableIsAutofillEnabledStateFlow.value = isAutofillEnabledAndSupported
            }
            .launchIn(unconfinedScope)
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
