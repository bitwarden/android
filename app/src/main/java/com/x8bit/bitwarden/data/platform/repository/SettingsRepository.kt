package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
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
}
