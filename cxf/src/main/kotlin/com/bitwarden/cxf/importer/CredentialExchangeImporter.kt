package com.bitwarden.cxf.importer

import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult

/**
 * Responsible for importing credentials from other apps.
 */
interface CredentialExchangeImporter {
    /**
     * Starts the import process by requesting selection of a source credential provider.
     *
     * @param credentialTypes The types of credentials to import.
     */
    suspend fun importCredentials(
        credentialTypes: List<String>,
    ): ImportCredentialsSelectionResult
}
