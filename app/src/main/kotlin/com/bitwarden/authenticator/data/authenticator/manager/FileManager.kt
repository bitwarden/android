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
}
