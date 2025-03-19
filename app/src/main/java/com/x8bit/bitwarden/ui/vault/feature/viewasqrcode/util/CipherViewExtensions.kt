package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.util

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.ViewAsQrCodeState
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeType
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model.QrCodeTypeField

/**
 * Converts the [CipherView] into a [ViewAsQrCodeState.ViewState.Content].
 */
//fun CipherView.toViewState(): ViewAsQrCodeState.ViewState.Content =
//    ViewAsQrCodeState.ViewState.Content(
//        //TODO map to Content
//        selectedQrCodeType = QrCodeType.PLAIN_TEXT,
//        qrCodeTypes = emptyList(),
//        qrCodeTypeFields = emptyMap<String, QrCodeTypeField>(),
//        cipherFields = emptyList(), //TODO UPDATE CIPHER LIST
//        //title = "From viewasqrcode.CipherViewExtensions.kt",
//        //TODO set fields list


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
//    )
