package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Primary implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val dispatcherManager: DispatcherManager,
) : SettingsRepository {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appLanguage: AppLanguage
        get() = settingsDiskSource.appLanguage ?: AppLanguage.DEFAULT
        set(value) {
            settingsDiskSource.appLanguage = value
        }

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

    override fun clearData(userId: String) {
        settingsDiskSource.clearData(userId = userId)
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
