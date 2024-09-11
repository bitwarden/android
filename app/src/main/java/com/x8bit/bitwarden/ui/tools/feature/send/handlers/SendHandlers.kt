package com.x8bit.bitwarden.ui.tools.feature.send.handlers

import com.x8bit.bitwarden.ui.tools.feature.send.SendAction
import com.x8bit.bitwarden.ui.tools.feature.send.SendState
import com.x8bit.bitwarden.ui.tools.feature.send.SendViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * send items.
 */
data class SendHandlers(
    val onTextTypeClick: () -> Unit,
    val onFileTypeClick: () -> Unit,
    val onSendClick: (SendState.ViewState.Content.SendItem) -> Unit,
    val onEditSendClick: (SendState.ViewState.Content.SendItem) -> Unit,
    val onCopySendClick: (SendState.ViewState.Content.SendItem) -> Unit,
    val onShareSendClick: (SendState.ViewState.Content.SendItem) -> Unit,
    val onDeleteSendClick: (SendState.ViewState.Content.SendItem) -> Unit,
    val onRemovePasswordClick: (SendState.ViewState.Content.SendItem) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [SendHandlers] by binding actions to the provided [SendViewModel].
         */
        fun create(
            viewModel: SendViewModel,
        ): SendHandlers =
            SendHandlers(
                onTextTypeClick = { viewModel.trySendAction(SendAction.TextTypeClick) },
                onFileTypeClick = { viewModel.trySendAction(SendAction.FileTypeClick) },
                onSendClick = { viewModel.trySendAction(SendAction.SendClick(it)) },
                onEditSendClick = { viewModel.trySendAction(SendAction.SendClick(it)) },
                onCopySendClick = { viewModel.trySendAction(SendAction.CopyClick(it)) },
                onShareSendClick = { viewModel.trySendAction(SendAction.ShareClick(it)) },
                onDeleteSendClick = { viewModel.trySendAction(SendAction.DeleteSendClick(it)) },
                onRemovePasswordClick = {
                    viewModel.trySendAction(SendAction.RemovePasswordClick(it))
                },
            )
    }
}
