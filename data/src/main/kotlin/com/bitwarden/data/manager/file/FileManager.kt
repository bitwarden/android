package com.bitwarden.data.manager.file

import android.net.Uri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.data.manager.model.DownloadResult
import com.bitwarden.data.manager.model.ZipFileResult
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
     * Absolute path to the private logs storage directory.
     */
    val logsDirectory: String

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
     * Reads the file or folder on disk from the [uri] and creates a temporary zip file. A
     * successful result will contain the zip [File] reference.
     */
    suspend fun zipUriToCache(uri: Uri): ZipFileResult

    /**
     * Reads the [fileUri] into a file on disk. A successful result will contain the [File]
     * reference.
     */
    suspend fun writeUriToCache(fileUri: Uri): Result<File>
}
