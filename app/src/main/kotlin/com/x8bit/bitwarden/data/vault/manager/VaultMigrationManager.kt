package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the migration of personal vault items to organization collections.
 * This interface provides a way to check if migration is needed and track migration state.
 */
interface VaultMigrationManager {
    /**
     * Flow that emits when conditions are met for the user to migrate their personal vault.
     * Updated after each sync to reflect current policy and vault state.
     */
    val vaultMigrationDataStateFlow: StateFlow<VaultMigrationData>

    /**
     * Verifies if the user should migrate their personal vault to organization collections
     * based on active policies, feature flags, and the provided cipher list.
     *
     * @param userId The ID of the user to check for migration
     * @param cipherList List of ciphers from the sync response to check for personal items.
     */
    fun verifyAndUpdateMigrationState(userId: String, cipherList: List<SyncResponseJson.Cipher>)

    /**
     * Checks if the user should migrate their vault based on policies, feature flags,
     * network connectivity, and whether they have personal items.
     *
     * @param hasPersonalItems Callback to check if the user has personal items.
     * @return true if migration conditions are met, false otherwise.
     */
    fun shouldMigrateVault(hasPersonalItems: () -> Boolean): Boolean
}
