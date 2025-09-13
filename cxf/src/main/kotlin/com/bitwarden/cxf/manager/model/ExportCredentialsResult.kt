package com.bitwarden.cxf.manager.model

import android.net.Uri
import androidx.credentials.providerevents.exception.ImportCredentialsException

/**
 * Represents the result of exporting credentials.
 */
sealed class ExportCredentialsResult {

    /**
     * Represents a successful export.
     */
    data class Success(val payload: String, val uri: Uri) : ExportCredentialsResult()

    /**
     * Represents a failure to export.
     */
    data class Failure(val error: ImportCredentialsException) : ExportCredentialsResult()
}
