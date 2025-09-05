package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.vault.Cipher

/**
 * Models result of the vault data being imported from a CXF payload.
 */
sealed class ImportCxfPayloadResult {

    /**
     * The vault data has been successfully imported.
     */
    data class Success(val ciphers: List<Cipher>) : ImportCxfPayloadResult()

    /**
     * There was an error importing the vault data.
     *
     * @param error The error that occurred during import.
     */
    data class Error(val error: Throwable) : ImportCxfPayloadResult()
}
