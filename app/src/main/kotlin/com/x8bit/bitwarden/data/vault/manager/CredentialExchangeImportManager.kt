package com.x8bit.bitwarden.data.vault.manager

import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult

/**
 * Manages the import process for Credential Exchange Format (CXF) payloads.
 *
 * This interface provides a contract for importing credential data from a standardized
 * CXF string, associating it with a specific user. It handles the parsing, decryption,
 * and storage of the credentials contained within the payload.
 */
interface CredentialExchangeImportManager {

    /**
     * Attempt to import a CXF payload.
     *
     * @param payload The CXF payload to import.
     */
    suspend fun importCxfPayload(userId: String, payload: String): ImportCxfPayloadResult
}
