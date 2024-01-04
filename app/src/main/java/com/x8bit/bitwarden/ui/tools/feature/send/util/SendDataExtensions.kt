package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.send.SendState

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Transforms [SendData] into [SendState.ViewState].
 */
fun SendData.toViewState(): SendState.ViewState =
    this
        .sendViewList
        .takeUnless { it.isEmpty() }
        ?.toSendContent()
        ?: SendState.ViewState.Empty

private fun List<SendView>.toSendContent(): SendState.ViewState.Content {
    return SendState.ViewState.Content(
        textTypeCount = this.count { it.type == SendType.TEXT },
        fileTypeCount = this.count { it.type == SendType.FILE },
        sendItems = this.map {
            SendState.ViewState.Content.SendItem(
                id = requireNotNull(it.id),
                name = it.name,
                deletionDate = it.deletionDate.toFormattedPattern(DELETION_DATE_PATTERN),
                type = when (it.type) {
                    SendType.TEXT -> SendState.ViewState.Content.SendItem.Type.TEXT
                    SendType.FILE -> SendState.ViewState.Content.SendItem.Type.FILE
                },
            )
        },
    )
}
