package com.x8bit.bitwarden.data.vault.repository

import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult

/**
 * Responsible for managing vault data inside the network layer.
 */
interface VaultRepository {

    /**
     * Attempt to sync the vault data.
     */
    fun sync()

    /**
     * Attempt to initialize crypto and sync the vault data.
     */
    suspend fun unlockVaultAndSync(masterPassword: String): VaultUnlockResult
}
