package com.bitwarden.cxf.importer.model

import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.exception.ImportCredentialsException

/**
 * Represents the result of the credential selection step of the import process.
 */
sealed class ImportCredentialsSelectionResult {

    /**
     * Represents a successful response from the selected credential provider.
     *
     * @property response The response from the import. This is a JSON string containing
     * credentials to import and is compliant with the FIDO2 CXF standard.
     * @property callingAppInfo The calling app information.
     */
    data class Success(
        val response: String,
        val callingAppInfo: CallingAppInfo,
    ) : ImportCredentialsSelectionResult()

    /**
     * Represents a cancellation of the import process.
     */
    data object Cancelled : ImportCredentialsSelectionResult()

    /**
     * Represents a failure during the credential selection step of the import process.
     *
     * @property error The exception that caused the failure.
     */
    data class Failure(
        val error: ImportCredentialsException,
    ) : ImportCredentialsSelectionResult()
}
