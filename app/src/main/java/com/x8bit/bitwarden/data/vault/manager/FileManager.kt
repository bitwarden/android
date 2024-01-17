package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Manages reading files.
 */
@OmitFromCoverage
interface FileManager {

    /**
     * Reads the [fileUri] into memory and returns the raw [ByteArray]
     */
    fun uriToByteArray(fileUri: Uri): ByteArray
}
