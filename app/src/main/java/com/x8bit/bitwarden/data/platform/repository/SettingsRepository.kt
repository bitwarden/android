package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Provides an API for observing and modifying settings state.
 */
@Suppress("TooManyFunctions")
interface SettingsRepository {
    /**
     * The [AppLanguage] for the current user.
     */
    var appLanguage: AppLanguage

    /**
     * The currently stored [AppTheme].
     */
    var appTheme: AppTheme

    /**
     * Tracks changes to the [AppTheme].
     */
    val appThemeStateFlow: StateFlow<AppTheme>

    /**
     * The currently stored last time the vault was synced.
     */
    var vaultLastSync: Instant?

    /**
     * Tracks changes to the [vaultLastSync].
     */
    val vaultLastSyncStateFlow: StateFlow<Instant?>

    /**
     * The current setting for getting login item icons.
     */
    var isIconLoadingDisabled: Boolean

    /**
     * Emits updates that track the [isIconLoadingDisabled] value.
     */
    val isIconLoadingDisabledFlow: Flow<Boolean>

    /**
     * The [VaultTimeout] for the current user.
     */
    var vaultTimeout: VaultTimeout

    /**
     * The [VaultTimeoutAction] for the current user.
     */
    var vaultTimeoutAction: VaultTimeoutAction

    /**
     * Whether or not PIN unlocking is enabled for the current user.
     */
    val isUnlockWithPinEnabled: Boolean

    /**
     * Whether or not inline autofill is enabled for the current user.
     */
    var isInlineAutofillEnabled: Boolean

    /**
     * A list of blocked autofill URI's for the current user.
     */
    var blockedAutofillUris: List<String>

    /**
     * Whether or not approving passwordless logins is enabled for the current user.
     */
    var isApprovePasswordlessLoginsEnabled: Boolean

    /**
     * Emits updates whenever there is a change in the app's status for supporting autofill.
     */
    val isAutofillEnabledStateFlow: StateFlow<Boolean>

    /**
     * Disables autofill if it is currently enabled.
     */
    fun disableAutofill()

    /**
     * Sets default values for various settings for the given [userId] if necessary. This is
     * typically used when logging into a new account.
     */
    fun setDefaultsIfNecessary(userId: String)

    /**
     * Gets updates for the [VaultTimeout] associated with the given [userId].
     */
    fun getVaultTimeoutStateFlow(userId: String): StateFlow<VaultTimeout>

    /**
     * Stores the given [vaultTimeout] for the given [userId].
     */
    fun storeVaultTimeout(userId: String, vaultTimeout: VaultTimeout)

    /**
     * Gets updates for the [VaultTimeoutAction] associated with the given [userId].
     *
     * Note that in cases where no value has been set, a default will be returned. Use
     * [isVaultTimeoutActionSet] to see if there is an actual value set.
     */
    fun getVaultTimeoutActionStateFlow(userId: String): StateFlow<VaultTimeoutAction>

    /**
     * Returns `true` if a [VaultTimeoutAction] is set for the given [userId], and `false`
     * otherwise.
     */
    fun isVaultTimeoutActionSet(userId: String): Boolean

    /**
     * Stores the given [VaultTimeoutAction] for the given [userId].
     */
    fun storeVaultTimeoutAction(userId: String, vaultTimeoutAction: VaultTimeoutAction?)

    /**
     * Gets updates for the pull to refresh enabled.
     */
    fun getPullToRefreshEnabledFlow(): StateFlow<Boolean>

    /**
     * Stores the given [isPullToRefreshEnabled] for the active user.
     */
    fun storePullToRefreshEnabled(isPullToRefreshEnabled: Boolean)

    /**
     * Stores the given PIN, allowing it to be used to unlock the current user's vault.
     *
     * When [shouldRequireMasterPasswordOnRestart] is `true`, the user's master password is required
     * on app startup but they may use their PIN to unlock their vault if it becomes locked while
     * the app is still open.
     */
    fun storeUnlockPin(
        pin: String,
        shouldRequireMasterPasswordOnRestart: Boolean,
    )

    /**
     * Clears any previously set unlock PIN for the current user.
     */
    fun clearUnlockPin()
}
