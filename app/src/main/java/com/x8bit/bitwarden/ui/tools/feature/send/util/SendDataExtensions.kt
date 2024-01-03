package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendView
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.SendState

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
    // TODO: Populate with real data BIT-481
    return SendState.ViewState.Content
}
