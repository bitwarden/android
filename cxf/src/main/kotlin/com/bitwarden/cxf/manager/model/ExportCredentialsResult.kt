package com.bitwarden.cxf.manager.model

import android.net.Uri
import androidx.credentials.providerevents.exception.ImportCredentialsException

/**
 * Represents the result of exporting credentials.
 */
sealed class ExportCredentialsResult {

    /**
     * Represents a successful export.
     *
     * @param payload The payload of the export, formatted as a FIDO 2
     * [Account Entity](https://fidoalliance.org/specs/cx/cxf-v1.0-ps-20250814.html#entity-account)
     * JSON string.
     */
    data class Success(val payload: String, val uri: Uri) : ExportCredentialsResult()

    /**
     * Represents a failure to export.
     */
    data class Failure(val error: ImportCredentialsException) : ExportCredentialsResult()
}
