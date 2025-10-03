package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Represents the result of importing credentials from a credential manager.
 */
sealed class ImportCredentialsResult {

    /**
     * Indicates the vault data has been successfully imported.
     */
    data class Success(val itemCount: Int) : ImportCredentialsResult()

    /**
     * Indicates there are no items to import.
     */
    data object NoItems : ImportCredentialsResult()

    /**
     * Indicates the vault data has been successfully uploaded, but there was an error syncing the
     * vault data.
     */
    data class SyncFailed(val error: Throwable) : ImportCredentialsResult()

    /**
     * Indicates there was an error importing the vault data.
     *
     * @param error The error that occurred during import.
     */
    data class Error(val error: Throwable) : ImportCredentialsResult()
}
