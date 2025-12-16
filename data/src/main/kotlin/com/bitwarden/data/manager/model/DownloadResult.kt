package com.bitwarden.data.manager.model

import java.io.File

/**
 * Represents a result from downloading a raw file.
 */
sealed class DownloadResult {
    /**
     * The download was a success, and was saved to [file].
     */
    data class Success(val file: File) : DownloadResult()

    /**
     * The download failed.
     */
    data class Failure(
        val error: Throwable,
    ) : DownloadResult()
}
