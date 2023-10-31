package com.x8bit.bitwarden.data.vault.repository

/**
 * Responsible for managing vault data inside the network layer.
 */
interface VaultRepository {

    /**
     * Attempt to sync the vault data.
     */
    suspend fun sync()
}
