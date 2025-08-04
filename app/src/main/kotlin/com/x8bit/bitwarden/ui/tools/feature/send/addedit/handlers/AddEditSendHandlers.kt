package com.x8bit.bitwarden.ui.tools.feature.send.addedit.handlers

import com.bitwarden.ui.platform.model.FileData
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendAction
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendViewModel
import java.time.ZonedDateTime

/**
 * A collection of handler functions for managing actions within the context of adding and editing
 * send items.
 */
data class AddEditSendHandlers(
    val onNameChange: (String) -> Unit,
    val onChooseFileClick: (hasPermission: Boolean) -> Unit,
    val onFileChoose: (FileData) -> Unit,
    val onTextChange: (String) -> Unit,
    val onIsHideByDefaultToggle: (Boolean) -> Unit,
    val onMaxAccessCountChange: (Int) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onHideEmailToggle: (Boolean) -> Unit,
    val onDeactivateSendToggle: (Boolean) -> Unit,
    val onDeletionDateChange: (ZonedDateTime) -> Unit,
    val onDeleteClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [AddEditSendHandlers] by binding actions to the provided
         * [AddEditSendViewModel].
         */
        fun create(
            viewModel: AddEditSendViewModel,
        ): AddEditSendHandlers =
            AddEditSendHandlers(
                onNameChange = { viewModel.trySendAction(AddEditSendAction.NameChange(it)) },
                onChooseFileClick = {
                    viewModel.trySendAction(AddEditSendAction.ChooseFileClick(it))
                },
                onFileChoose = { viewModel.trySendAction(AddEditSendAction.FileChoose(it)) },
                onTextChange = { viewModel.trySendAction(AddEditSendAction.TextChange(it)) },
                onIsHideByDefaultToggle = {
                    viewModel.trySendAction(AddEditSendAction.HideByDefaultToggle(it))
                },
                onMaxAccessCountChange = {
                    viewModel.trySendAction(AddEditSendAction.MaxAccessCountChange(it))
                },
                onPasswordChange = {
                    viewModel.trySendAction(AddEditSendAction.PasswordChange(it))
                },
                onNoteChange = { viewModel.trySendAction(AddEditSendAction.NoteChange(it)) },
                onHideEmailToggle = {
                    viewModel.trySendAction(AddEditSendAction.HideMyEmailToggle(it))
                },
                onDeactivateSendToggle = {
                    viewModel.trySendAction(AddEditSendAction.DeactivateThisSendToggle(it))
                },
                onDeletionDateChange = {
                    viewModel.trySendAction(AddEditSendAction.DeletionDateChange(it))
                },
                onDeleteClick = { viewModel.trySendAction(AddEditSendAction.DeleteClick) },
            )
    }
}
