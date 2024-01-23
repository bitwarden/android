package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Primary access point for general settings-related disk information.
 */
@Suppress("TooManyFunctions")
interface SettingsDiskSource {

    /**
     * The currently persisted app language (or `null` if not set).
     */
    var appLanguage: AppLanguage?

    /**
     * The currently persisted app theme (or `null` if not set).
     */
    var appTheme: AppTheme

    /**
     * Emits updates that track [appTheme].
     */
    val appThemeFlow: Flow<AppTheme>

    /**
     * The currently persisted setting for getting login item icons (or `null` if not set).
     */
    var isIconLoadingDisabled: Boolean?

    /**
     * Emits updates that track [isIconLoadingDisabled].
     */
    val isIconLoadingDisabledFlow: Flow<Boolean?>

    /**
     * Clears all the settings data for the given user.
     */
    fun clearData(userId: String)

    /**
     * Gets the last time the app synced the vault data for a given [userId] (or `null` if the
     * vault has never been synced).
     */
    fun getLastSyncTime(userId: String): Instant?

    /**
     * Emits updates that track [getLastSyncTime] for the given [userId]. This will replay the
     * last known value, if any.
     */
    fun getLastSyncTimeFlow(userId: String): Flow<Instant?>

    /**
     * Stores the given [lastSyncTime] for the given [userId].
     */
    fun storeLastSyncTime(userId: String, lastSyncTime: Instant?)

    /**
     * Gets the current vault timeout (in minutes) for the given [userId] (or `null` if the vault
     * should never time out).
     */
    fun getVaultTimeoutInMinutes(userId: String): Int?

    /**
     * Emits updates that track [getVaultTimeoutInMinutes] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getVaultTimeoutInMinutesFlow(userId: String): Flow<Int?>

    /**
     * Stores the given [vaultTimeoutInMinutes] for the given [userId].
     */
    fun storeVaultTimeoutInMinutes(userId: String, vaultTimeoutInMinutes: Int?)

    /**
     * Gets the current [VaultTimeoutAction] for the given [userId].
     */
    fun getVaultTimeoutAction(userId: String): VaultTimeoutAction?

    /**
     * Emits updates that track [getVaultTimeoutAction] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getVaultTimeoutActionFlow(userId: String): Flow<VaultTimeoutAction?>

    /**
     * Stores the given [vaultTimeoutAction] for the given [userId].
     */
    fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    )

    /**
     * Gets the current state of the pull to refresh feature for the given [userId].
     */
    fun getPullToRefreshEnabled(userId: String): Boolean?

    /**
     * Emits updates that track [getPullToRefreshEnabled] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getPullToRefreshEnabledFlow(userId: String): Flow<Boolean?>

    /**
     * Stores the given [isPullToRefreshEnabled] for the given [userId].
     */
    fun storePullToRefreshEnabled(userId: String, isPullToRefreshEnabled: Boolean?)

    /**
     * Gets the value determining if inline autofill is enabled for the given [userId].
     */
    fun getInlineAutofillEnabled(userId: String): Boolean?

    /**
     * Stores the given [isInlineAutofillEnabled] value for the given [userId].
     */
    fun storeInlineAutofillEnabled(userId: String, isInlineAutofillEnabled: Boolean?)

    /**
     * Gets a list of blocked autofill URI's for the given [userId].
     */
    fun getBlockedAutofillUris(userId: String): List<String>?

    /**
     * Stores the list of [blockedAutofillUris] for the given [userId].
     */
    fun storeBlockedAutofillUris(
        userId: String,
        blockedAutofillUris: List<String>?,
    )

    /**
     * Gets whether or not the given [userId] has enabled approving passwordless logins.
     */
    fun getApprovePasswordlessLoginsEnabled(userId: String): Boolean?

    /**
     * Stores whether or not [isApprovePasswordlessLoginsEnabled] for the given [userId].
     */
    fun storeApprovePasswordlessLoginsEnabled(
        userId: String,
        isApprovePasswordlessLoginsEnabled: Boolean?,
    )
}
