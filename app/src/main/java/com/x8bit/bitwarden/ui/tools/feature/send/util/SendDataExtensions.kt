package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import java.time.Instant

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Transforms [SendData] into [SendState.ViewState].
 */
fun SendData.toViewState(
    baseWebSendUrl: String,
): SendState.ViewState =
    this
        .sendViewList
        .takeUnless { it.isEmpty() }
        ?.toSendContent(baseWebSendUrl)
        ?: SendState.ViewState.Empty

private fun List<SendView>.toSendContent(
    baseWebSendUrl: String,
): SendState.ViewState.Content {
    return SendState.ViewState.Content(
        textTypeCount = this.count { it.type == SendType.TEXT },
        fileTypeCount = this.count { it.type == SendType.FILE },
        sendItems = this
            .map { sendView ->
                SendState.ViewState.Content.SendItem(
                    id = requireNotNull(sendView.id),
                    name = sendView.name,
                    deletionDate = sendView.deletionDate.toFormattedPattern(DELETION_DATE_PATTERN),
                    type = when (sendView.type) {
                        SendType.TEXT -> SendState.ViewState.Content.SendItem.Type.TEXT
                        SendType.FILE -> SendState.ViewState.Content.SendItem.Type.FILE
                    },
                    iconList = listOfNotNull(
                        SendStatusIcon.DISABLED.takeIf { sendView.disabled },
                        SendStatusIcon.PASSWORD.takeIf { sendView.hasPassword },
                        SendStatusIcon.MAX_ACCESS_COUNT_REACHED.takeIf {
                            sendView.maxAccessCount?.let { maxCount ->
                                sendView.accessCount >= maxCount
                            } == true
                        },
                        SendStatusIcon.EXPIRED.takeIf {
                            sendView.expirationDate?.isBefore(Instant.now()) == true
                        },
                        SendStatusIcon.PENDING_DELETE.takeIf {
                            sendView.deletionDate.isBefore(Instant.now())
                        },
                    ),
                    shareUrl = sendView.toSendUrl(baseWebSendUrl),
                )
            }
            .sortedBy { it.name },
    )
}
