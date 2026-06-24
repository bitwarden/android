package com.bitwarden.cxf.importer

import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult

/**
 * Responsible for importing credentials from other apps.
 */
interface CredentialExchangeImporter {
    /**
     * Returns `true` if credential exchange import is available on the current build, or `false`
     * if it is unsupported (e.g. on builds without the Credential Exchange backend). Callers
     * should hide or disable import entry points when this returns `false`.
     */
    fun isSupported(): Boolean

    /**
     * Starts the import process by requesting selection of a source credential provider.
     *
     * @param credentialTypes The types of credentials to import.
     */
    suspend fun importCredentials(
        credentialTypes: List<String>,
    ): ImportCredentialsSelectionResult
}
