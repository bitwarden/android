package com.bitwarden.data.manager.model

import java.io.File

/**
 * Represents a result from zipping a raw file or folder.
 */
sealed class ZipFileResult {
    /**
     * The zip was a success, and was saved to the temporary [file].
     */
    data class Success(val file: File) : ZipFileResult()

    /**
     * There was no file or folder to zip.
     */
    data object NothingToZip : ZipFileResult()

    /**
     * The zip failed in some generic manner.
     */
    data class Failure(val error: Throwable) : ZipFileResult()
}
