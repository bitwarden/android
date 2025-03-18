package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.ViewAsQrCodeState

/**
 * Converts the [CipherView] into a [ViewAsQrCodeState.ViewState.Content].
 */
fun CipherView.toViewState(): ViewAsQrCodeState.ViewState.Content =
    ViewAsQrCodeState.ViewState.Content(
        //TODO map to Content
        title = "From viewasqrcode.CipherViewExtensions.kt"

//        originalCipher = this,
//        attachments = this
//            .attachments
//            .orEmpty()
//            .mapNotNull {
//                val id = it.id ?: return@mapNotNull null
//                AttachmentsState.AttachmentItem(
//                    id = id,
//                    title = it.fileName.orEmpty(),
//                    displaySize = it.sizeName.orEmpty(),
//                )
//            },
//        newAttachment = null,
    )
