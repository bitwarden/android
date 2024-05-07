package com.bitwarden.authenticator.data.authenticator.manager

import android.net.Uri

/**
 * Manages reading and writing files.
 */
interface FileManager {

    /**
     * Writes the given [dataString] to disk at the provided [fileUri]
     */
    suspend fun stringToUri(fileUri: Uri, dataString: String): Boolean

    /**
     * Reads the [fileUri] into memory. A successful result will contain the raw [ByteArray].
     */
    suspend fun uriToByteArray(fileUri: Uri): Result<ByteArray>
}
