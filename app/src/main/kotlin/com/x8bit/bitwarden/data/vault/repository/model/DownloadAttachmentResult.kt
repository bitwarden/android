package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage
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
     * The attachment could not be downloaded. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Failure(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : DownloadAttachmentResult()
}
