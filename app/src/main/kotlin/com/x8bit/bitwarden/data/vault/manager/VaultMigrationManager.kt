package com.x8bit.bitwarden.data.vault.manager

import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
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
}
