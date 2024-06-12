package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.vault.CipherView

/**
 * Models result of creating an attachment.
 */
sealed class CreateAttachmentResult {

    /**
     * Attachment created successfully.
     */
    data class Success(
        val cipherView: CipherView,
    ) : CreateAttachmentResult()

    /**
     * Generic error while creating an attachment.
     */
    data object Error : CreateAttachmentResult()
}
