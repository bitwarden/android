package com.x8bit.bitwarden.ui.tools.feature.send.viewsend.util

import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.send.SendFileView
import com.bitwarden.send.SendTextView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendState
import java.time.Clock
import java.time.format.FormatStyle

/**
 * Transforms the given [SendView] to a [ViewSendState.ViewState.Content].
 */
fun SendView.toViewSendViewStateContent(
    baseWebSendUrl: String,
    clock: Clock,
): ViewSendState.ViewState.Content =
    ViewSendState.ViewState.Content(
        sendType = when (this.type) {
            SendType.FILE -> requireNotNull(this.file).toFileType()
            SendType.TEXT -> requireNotNull(this.text).toTextType()
        },
        shareLink = this.toSendUrl(baseWebSendUrl = baseWebSendUrl),
        sendName = this.name,
        deletionDate = this.deletionDate.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.MEDIUM,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
        maxAccessCount = this.maxAccessCount?.toInt(),
        currentAccessCount = this.accessCount.toInt(),
        notes = this.notes,
    )

private fun SendFileView.toFileType(): ViewSendState.ViewState.Content.SendType.FileType =
    ViewSendState.ViewState.Content.SendType.FileType(
        fileName = this.fileName,
        fileSize = this.sizeName.orEmpty(),
    )

private fun SendTextView.toTextType(): ViewSendState.ViewState.Content.SendType.TextType =
    ViewSendState.ViewState.Content.SendType.TextType(
        textToShare = this.text.orEmpty(),
    )
