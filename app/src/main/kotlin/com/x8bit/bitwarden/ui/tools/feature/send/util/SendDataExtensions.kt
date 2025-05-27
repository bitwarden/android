package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import java.time.Clock

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Transforms [SendData] into [SendState.ViewState].
 */
fun SendData.toViewState(
    baseWebSendUrl: String,
    clock: Clock = Clock.systemDefaultZone(),
): SendState.ViewState =
    this
        .sendViewList
        .takeUnless { it.isEmpty() }
        ?.toSendContent(baseWebSendUrl, clock)
        ?: SendState.ViewState.Empty

private fun List<SendView>.toSendContent(
    baseWebSendUrl: String,
    clock: Clock,
): SendState.ViewState.Content {
    return SendState.ViewState.Content(
        textTypeCount = this.count { it.type == SendType.TEXT },
        fileTypeCount = this.count { it.type == SendType.FILE },
        sendItems = this
            .map { sendView ->
                SendState.ViewState.Content.SendItem(
                    id = requireNotNull(sendView.id),
                    name = sendView.name,
                    deletionDate = sendView.deletionDate.toFormattedPattern(
                        pattern = DELETION_DATE_PATTERN,
                        clock = clock,
                    ),
                    type = when (sendView.type) {
                        SendType.TEXT -> SendState.ViewState.Content.SendItem.Type.TEXT
                        SendType.FILE -> SendState.ViewState.Content.SendItem.Type.FILE
                    },
                    iconList = sendView.toLabelIcons(),
                    shareUrl = sendView.toSendUrl(baseWebSendUrl),
                    hasPassword = sendView.hasPassword,
                )
            },
    )
}
