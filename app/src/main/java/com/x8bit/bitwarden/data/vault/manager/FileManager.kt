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
     * Deletes a [file] from the system.
     */
    suspend fun deleteFile(file: File)

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
     * Reads the [fileUri] into memory and returns the raw [ByteArray]
     */
    suspend fun uriToByteArray(fileUri: Uri): ByteArray
}
