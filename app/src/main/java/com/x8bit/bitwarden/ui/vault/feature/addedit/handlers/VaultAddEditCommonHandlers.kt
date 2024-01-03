package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType

/**
 * A collection of handler functions for managing actions common
 * within the context of adding items to a vault.
 *
 * @property onNameTextChange Handles the action when the name text is changed.
 * @property onFolderTextChange Handles the action when the folder text is changed.
 * @property onToggleFavorite Handles the action when the favorite toggle is changed.
 * @property onToggleMasterPasswordReprompt Handles the action when the master password
 * reprompt toggle is changed.
 * @property onNotesTextChange Handles the action when the notes text is changed.
 * @property onOwnershipTextChange Handles the action when the ownership text is changed.
 * @property onTooltipClick Handles the action when the tooltip button is clicked.
 * @property onAddNewCustomFieldClick Handles the action when the add new custom field
 * button is clicked.
 * @property onCustomFieldValueChange Handles the action when the field's value changes
 */
@Suppress("LongParameterList")
class VaultAddEditCommonHandlers(
    val onNameTextChange: (String) -> Unit,
    val onFolderTextChange: (String) -> Unit,
    val onToggleFavorite: (Boolean) -> Unit,
    val onToggleMasterPasswordReprompt: (Boolean) -> Unit,
    val onNotesTextChange: (String) -> Unit,
    val onOwnershipTextChange: (String) -> Unit,
    val onTooltipClick: () -> Unit,
    val onAddNewCustomFieldClick: (CustomFieldType, String) -> Unit,
    val onCustomFieldValueChange: (VaultAddEditState.Custom) -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [VaultAddEditCommonHandlers] by binding actions
         * to the provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditCommonHandlers {
            return VaultAddEditCommonHandlers(
                onNameTextChange = { newName ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.NameTextChange(newName),
                    )
                },
                onFolderTextChange = { newFolder ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.FolderChange(
                            newFolder.asText(),
                        ),
                    )
                },
                onToggleFavorite = { isFavorite ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.ToggleFavorite(isFavorite),
                    )
                },
                onToggleMasterPasswordReprompt = { isMasterPasswordReprompt ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.ToggleMasterPasswordReprompt(
                            isMasterPasswordReprompt,
                        ),
                    )
                },
                onNotesTextChange = { newNotes ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.NotesTextChange(newNotes),
                    )
                },
                onOwnershipTextChange = { newOwnership ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.OwnershipChange(newOwnership),
                    )
                },
                onTooltipClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.TooltipClick,
                    )
                },
                onAddNewCustomFieldClick = { newCustomFieldType, name ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.AddNewCustomFieldClick(
                            newCustomFieldType,
                            name,
                        ),
                    )
                },
                onCustomFieldValueChange = { newValue ->
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.CustomFieldValueChange(
                            newValue,
                        ),
                    )
                },
            )
        }
    }
}
