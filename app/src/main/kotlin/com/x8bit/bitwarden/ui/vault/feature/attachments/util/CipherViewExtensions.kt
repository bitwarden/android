package com.x8bit.bitwarden.ui.vault.feature.attachments.util

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.vault.feature.attachments.AttachmentsState

/**
 * Converts the [CipherView] into a [AttachmentsState.ViewState.Content].
 */
fun CipherView.toViewState(): AttachmentsState.ViewState.Content =
    AttachmentsState.ViewState.Content(
        originalCipher = this,
        attachments = this
            .attachments
            .orEmpty()
            .mapNotNull {
                val id = it.id ?: return@mapNotNull null
                AttachmentsState.AttachmentItem(
                    id = id,
                    title = it.fileName.orEmpty(),
                    displaySize = it.sizeName.orEmpty(),
                )
            },
        newAttachment = null,
    )
