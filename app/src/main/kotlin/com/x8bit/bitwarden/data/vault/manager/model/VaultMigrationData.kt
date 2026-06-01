package com.x8bit.bitwarden.data.vault.manager.model

/**
 * Represents vault migration state with organization metadata.
 */
sealed class VaultMigrationData {
    /**
     * User should migrate personal vault items to the specified organization.
     */
    data class MigrationRequired(
        val organizationId: String,
        val organizationName: String,
    ) : VaultMigrationData()

    /**
     * No migration required.
     */
    data object NoMigrationRequired : VaultMigrationData()
}
