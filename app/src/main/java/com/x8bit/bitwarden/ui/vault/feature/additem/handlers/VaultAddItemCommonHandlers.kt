package com.x8bit.bitwarden.ui.vault.feature.additem.handlers

import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemAction
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemViewModel
import com.x8bit.bitwarden.ui.vault.feature.additem.model.CustomFieldType

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
class VaultAddItemCommonHandlers(
    val onNameTextChange: (String) -> Unit,
    val onFolderTextChange: (String) -> Unit,
    val onToggleFavorite: (Boolean) -> Unit,
    val onToggleMasterPasswordReprompt: (Boolean) -> Unit,
    val onNotesTextChange: (String) -> Unit,
    val onOwnershipTextChange: (String) -> Unit,
    val onTooltipClick: () -> Unit,
    val onAddNewCustomFieldClick: (CustomFieldType, String) -> Unit,
    val onCustomFieldValueChange: (VaultAddItemState.Custom) -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [VaultAddItemCommonHandlers] by binding actions
         * to the provided [VaultAddItemViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddItemViewModel): VaultAddItemCommonHandlers {
            return VaultAddItemCommonHandlers(
                onNameTextChange = { newName ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.NameTextChange(newName),
                    )
                },
                onFolderTextChange = { newFolder ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.FolderChange(
                            newFolder.asText(),
                        ),
                    )
                },
                onToggleFavorite = { isFavorite ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.ToggleFavorite(isFavorite),
                    )
                },
                onToggleMasterPasswordReprompt = { isMasterPasswordReprompt ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.ToggleMasterPasswordReprompt(
                            isMasterPasswordReprompt,
                        ),
                    )
                },
                onNotesTextChange = { newNotes ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.NotesTextChange(newNotes),
                    )
                },
                onOwnershipTextChange = { newOwnership ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.OwnershipChange(newOwnership),
                    )
                },
                onTooltipClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.TooltipClick,
                    )
                },
                onAddNewCustomFieldClick = { newCustomFieldType, name ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.AddNewCustomFieldClick(
                            newCustomFieldType,
                            name,
                        ),
                    )
                },
                onCustomFieldValueChange = { newValue ->
                    viewModel.trySendAction(
                        VaultAddItemAction.Common.CustomFieldValueChange(
                            newValue,
                        ),
                    )
                },
            )
        }
    }
}
