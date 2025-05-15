package com.x8bit.bitwarden.ui.tools.feature.send.addsend.handlers

import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendAction
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendViewModel
import java.time.ZonedDateTime

/**
 * A collection of handler functions for managing actions within the context of adding
 * send items.
 */
data class AddSendHandlers(
    val onNameChange: (String) -> Unit,
    val onChooseFileClick: (hasPermission: Boolean) -> Unit,
    val onFileChoose: (IntentManager.FileData) -> Unit,
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
         * Creates an instance of [AddSendHandlers] by binding actions to the provided
         * [AddSendViewModel].
         */
        fun create(
            viewModel: AddSendViewModel,
        ): AddSendHandlers =
            AddSendHandlers(
                onNameChange = { viewModel.trySendAction(AddSendAction.NameChange(it)) },
                onChooseFileClick = { viewModel.trySendAction(AddSendAction.ChooseFileClick(it)) },
                onFileChoose = { viewModel.trySendAction(AddSendAction.FileChoose(it)) },
                onTextChange = { viewModel.trySendAction(AddSendAction.TextChange(it)) },
                onIsHideByDefaultToggle = {
                    viewModel.trySendAction(AddSendAction.HideByDefaultToggle(it))
                },
                onMaxAccessCountChange = {
                    viewModel.trySendAction(AddSendAction.MaxAccessCountChange(it))
                },
                onPasswordChange = { viewModel.trySendAction(AddSendAction.PasswordChange(it)) },
                onNoteChange = { viewModel.trySendAction(AddSendAction.NoteChange(it)) },
                onHideEmailToggle = {
                    viewModel.trySendAction(AddSendAction.HideMyEmailToggle(it))
                },
                onDeactivateSendToggle = {
                    viewModel.trySendAction(AddSendAction.DeactivateThisSendToggle(it))
                },
                onDeletionDateChange = {
                    viewModel.trySendAction(AddSendAction.DeletionDateChange(it))
                },
                onDeleteClick = { viewModel.trySendAction(AddSendAction.DeleteClick) },
            )
    }
}
