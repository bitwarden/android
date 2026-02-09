package com.x8bit.bitwarden.data.vault.manager

import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the migration of personal vault items to organization collections.
 * This interface provides a way to check if migration is needed and track migration state.
 *
 * The manager reactively observes vault cipher data and automatically updates the migration state
 * when conditions change (e.g., after sync, after vault unlock, policy changes).
 */
interface VaultMigrationManager {
    /**
     * Flow that emits when conditions are met for the user to migrate their personal vault.
     * Automatically updated when cipher data, policies, or feature flags change.
     */
    val vaultMigrationDataStateFlow: StateFlow<VaultMigrationData>

    /**
     * Migrates all personal vault items to the specified organization.
     *
     * @param userId The ID of the user performing the migration.
     * @param organizationId The ID of the organization to migrate items to.
     * @return Result indicating success or failure of the migration operation.
     */
    suspend fun migratePersonalVault(
        userId: String,
        organizationId: String,
    ): MigratePersonalVaultResult

    /**
     * Clears the migration state, setting it to [VaultMigrationData.NoMigrationRequired].
     * This should be called when the user declines migration or leaves the organization.
     */
    fun clearMigrationState()
}
