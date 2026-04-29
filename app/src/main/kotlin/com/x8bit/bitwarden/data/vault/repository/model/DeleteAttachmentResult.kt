package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of deleting an attachment from a cipher.
 */
sealed class DeleteAttachmentResult {

    /**
     * Attachment deleted successfully.
     */
    data object Success : DeleteAttachmentResult()

    /**
     * Generic error while deleting an attachment. The optional [errorMessage] may be
     * displayed directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : DeleteAttachmentResult()
}
