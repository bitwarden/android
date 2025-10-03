package com.x8bit.bitwarden.data.vault.manager.model

/**
 * Models result of the vault data being imported from a CXF payload.
 */
sealed class ImportCxfPayloadResult {

    /**
     * The vault data has been successfully imported.
     */
    data class Success(val itemCount: Int) : ImportCxfPayloadResult()

    /**
     * There are no items to import.
     */
    data object NoItems : ImportCxfPayloadResult()

    /**
     * The sync process has failed after importing the CXF payload.
     */
    data class SyncFailed(val error: Throwable) : ImportCxfPayloadResult()

    /**
     * There was an error importing the vault data.
     *
     * @param error The error that occurred during import.
     */
    data class Error(val error: Throwable) : ImportCxfPayloadResult()
}
