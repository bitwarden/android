package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of deleting an attachment from a cipher.
 */
sealed class DeleteAttachmentResult {

    /**
     * Attachment deleted successfully.
     */
    data object Success : DeleteAttachmentResult()

    /**
     * Generic error while deleting an attachment.
     */
    data object Error : DeleteAttachmentResult()
}
