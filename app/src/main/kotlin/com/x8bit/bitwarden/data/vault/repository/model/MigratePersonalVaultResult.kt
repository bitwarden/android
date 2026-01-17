package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of migrating the personal vault.
 */
sealed class MigratePersonalVaultResult {
    /**
     * Personal vault migrated successfully.
     */
    data object Success : MigratePersonalVaultResult()

    /**
     * Generic error while migrating personal vault
     */
    data class Failure(val error: Throwable?) : MigratePersonalVaultResult()
}
