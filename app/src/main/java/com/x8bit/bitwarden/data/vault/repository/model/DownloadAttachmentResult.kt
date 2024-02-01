package com.x8bit.bitwarden.data.vault.repository.model

import java.io.File

/**
 * Represents the overall result when attempting to download an attachment.
 */
sealed class DownloadAttachmentResult {
    /**
     * The attachment was successfully downloaded and saved to [file].
     */
    data class Success(val file: File) : DownloadAttachmentResult()

    /**
     * The attachment could not be downloaded.
     */
    data object Failure : DownloadAttachmentResult()
}
