package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing and modifying settings state.
 */
interface SettingsRepository {
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
}
