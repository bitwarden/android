package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for general settings-related disk information.
 */
interface SettingsDiskSource {

    /**
     * The currently persisted app language (or `null` if not set).
     */
    var appLanguage: AppLanguage?

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
}
