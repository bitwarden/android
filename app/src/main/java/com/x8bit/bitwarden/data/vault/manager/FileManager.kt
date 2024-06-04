package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.vault.manager.model.DownloadResult
import java.io.File

/**
 * Manages reading files.
 */
@OmitFromCoverage
interface FileManager {

    /**
     * Absolute path to the private file storage directory.
     */
    val filesDirectory: String

    /**
     * Deletes [files] from disk.
     */
    suspend fun delete(vararg files: File)

    /**
     * Downloads a file temporarily to cache from [url]. A successful [DownloadResult] will contain
     * the final file path.
     */
    suspend fun downloadFileToCache(url: String): DownloadResult

    /**
     * Writes an existing [file] to a [fileUri]. `true` will be returned if the file was
     * successfully saved.
     */
    suspend fun fileToUri(fileUri: Uri, file: File): Boolean

    /**
     * Writes an [dataString] to a [fileUri]. `true` will be returned if the file was
     * successfully saved.
     */
    suspend fun stringToUri(fileUri: Uri, dataString: String): Boolean

    /**
     * Reads the [fileUri] into memory. A successful result will contain the raw [ByteArray].
     */
    suspend fun uriToByteArray(fileUri: Uri): Result<ByteArray>

    /**
     * Reads the [fileUri] into a file on disk. A successful result will contain the [File]
     * reference.
     */
    suspend fun writeUriToCache(fileUri: Uri): Result<File>
}
