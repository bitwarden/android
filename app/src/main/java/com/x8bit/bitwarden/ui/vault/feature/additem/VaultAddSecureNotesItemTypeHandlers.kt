package com.x8bit.bitwarden.ui.vault.feature.additem

import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding secure note items to a vault.
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
 */
@Suppress("LongParameterList")
class VaultAddSecureNotesItemTypeHandlers(
    val onNameTextChange: (String) -> Unit,
    val onFolderTextChange: (String) -> Unit,
    val onToggleFavorite: (Boolean) -> Unit,
    val onToggleMasterPasswordReprompt: (Boolean) -> Unit,
    val onNotesTextChange: (String) -> Unit,
    val onOwnershipTextChange: (String) -> Unit,
    val onTooltipClick: () -> Unit,
    val onAddNewCustomFieldClick: () -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [VaultAddSecureNotesItemTypeHandlers] by binding actions
         * to the provided [VaultAddItemViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddItemViewModel): VaultAddSecureNotesItemTypeHandlers {
            return VaultAddSecureNotesItemTypeHandlers(
                onNameTextChange = { newName ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.NameTextChange(newName),
                    )
                },
                onFolderTextChange = { newFolder ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.FolderChange(
                            newFolder.asText(),
                        ),
                    )
                },
                onToggleFavorite = { isFavorite ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite(isFavorite),
                    )
                },
                onToggleMasterPasswordReprompt = { isMasterPasswordReprompt ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt(
                            isMasterPasswordReprompt,
                        ),
                    )
                },
                onNotesTextChange = { newNotes ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange(newNotes),
                    )
                },
                onOwnershipTextChange = { newOwnership ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange(newOwnership),
                    )
                },
                onTooltipClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.TooltipClick,
                    )
                },
                onAddNewCustomFieldClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.SecureNotesType.AddNewCustomFieldClick,
                    )
                },
            )
        }
    }
}
