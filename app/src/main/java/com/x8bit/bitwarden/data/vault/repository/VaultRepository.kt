package com.x8bit.bitwarden.data.vault.repository

import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing vault data inside the network layer.
 */
interface VaultRepository {

    /**
     * Flow that represents the current vault data.
     */
    val vaultDataStateFlow: StateFlow<DataState<VaultData>>

    /**
     * Flow that represents the current send data.
     */
    val sendDataStateFlow: StateFlow<DataState<SendData>>

    /**
     * Clear any previously unlocked, in-memory data (vault, send, etc).
     */
    fun clearUnlockedData()

    /**
     * Attempt to sync the vault data.
     */
    fun sync()

    /**
     * Attempt to initialize crypto and sync the vault data.
     */
    suspend fun unlockVaultAndSync(masterPassword: String): VaultUnlockResult
}
