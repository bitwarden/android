package com.x8bit.bitwarden.ui.vault.feature.attachments.handlers

import com.x8bit.bitwarden.ui.vault.feature.attachments.AttachmentsAction
import com.x8bit.bitwarden.ui.vault.feature.attachments.AttachmentsViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing attachments.
 */
data class AttachmentsHandlers(
    val onBackClick: () -> Unit,
    val onSaveClick: () -> Unit,
) {
    companion object {
        /**
         * Creates the [AttachmentsHandlers] using the [AttachmentsViewModel] to send desired
         * actions.
         */
        fun create(viewModel: AttachmentsViewModel): AttachmentsHandlers =
            AttachmentsHandlers(
                onBackClick = { viewModel.trySendAction(AttachmentsAction.BackClick) },
                onSaveClick = { viewModel.trySendAction(AttachmentsAction.SaveClick) },
            )
    }
}
