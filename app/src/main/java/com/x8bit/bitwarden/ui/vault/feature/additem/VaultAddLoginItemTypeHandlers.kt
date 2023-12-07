package com.x8bit.bitwarden.ui.vault.feature.additem

import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding login items to a vault.
 *
 * @property onNameTextChange Handles the action when the name text is changed.
 * @property onUsernameTextChange Handles the action when the username text is changed.
 * @property onPasswordTextChange Handles the action when the password text is changed.
 * @property onUriTextChange Handles the action when the URI text is changed.
 * @property onFolderTextChange Handles the action when the folder text is changed.
 * @property onToggleFavorite Handles the action when the favorite toggle is changed.
 * @property onToggleMasterPasswordReprompt Handles the action when the master password
 * reprompt toggle is changed.
 * @property onNotesTextChange Handles the action when the notes text is changed.
 * @property onOwnershipTextChange Handles the action when the ownership text is changed.
 * @property onOpenUsernameGeneratorClick Handles the action when the username generator
 * button is clicked.
 * @property onPasswordCheckerClick Handles the action when the password checker
 * button is clicked.
 * @property onOpenPasswordGeneratorClick Handles the action when the password generator
 * button is clicked.
 * @property onSetupTotpClick Handles the action when the setup TOTP button is clicked.
 * @property onUriSettingsClick Handles the action when the URI settings button is clicked.
 * @property onAddNewUriClick Handles the action when the add new URI button is clicked.
 * @property onTooltipClick Handles the action when the tooltip button is clicked.
 * @property onAddNewCustomFieldClick Handles the action when the add new custom field
 * button is clicked.
 */
@Suppress("LongParameterList")
class VaultAddLoginItemTypeHandlers(
    val onNameTextChange: (String) -> Unit,
    val onUsernameTextChange: (String) -> Unit,
    val onPasswordTextChange: (String) -> Unit,
    val onUriTextChange: (String) -> Unit,
    val onFolderTextChange: (String) -> Unit,
    val onToggleFavorite: (Boolean) -> Unit,
    val onToggleMasterPasswordReprompt: (Boolean) -> Unit,
    val onNotesTextChange: (String) -> Unit,
    val onOwnershipTextChange: (String) -> Unit,
    val onOpenUsernameGeneratorClick: () -> Unit,
    val onPasswordCheckerClick: () -> Unit,
    val onOpenPasswordGeneratorClick: () -> Unit,
    val onSetupTotpClick: () -> Unit,
    val onUriSettingsClick: () -> Unit,
    val onAddNewUriClick: () -> Unit,
    val onTooltipClick: () -> Unit,
    val onAddNewCustomFieldClick: () -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [VaultAddLoginItemTypeHandlers] by binding actions
         * to the provided [VaultAddItemViewModel].
         *
         * @param viewModel The [VaultAddItemViewModel] to which actions will be sent.
         * @return A fully initialized [VaultAddLoginItemTypeHandlers] object.
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddItemViewModel): VaultAddLoginItemTypeHandlers {
            return VaultAddLoginItemTypeHandlers(
                onNameTextChange = { newName ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.NameTextChange(newName),
                    )
                },
                onUsernameTextChange = { newUsername ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.UsernameTextChange(newUsername),
                    )
                },
                onPasswordTextChange = { newPassword ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.PasswordTextChange(newPassword),
                    )
                },
                onUriTextChange = { newUri ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.UriTextChange(newUri),
                    )
                },
                onFolderTextChange = { newFolder ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.FolderChange(newFolder.asText()),
                    )
                },
                onToggleFavorite = { isFavorite ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.ToggleFavorite(isFavorite),
                    )
                },
                onToggleMasterPasswordReprompt = { isMasterPasswordReprompt ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt(
                            isMasterPasswordReprompt,
                        ),
                    )
                },
                onNotesTextChange = { newNotes ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.NotesTextChange(newNotes),
                    )
                },
                onOwnershipTextChange = { newOwnership ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.OwnershipChange(newOwnership),
                    )
                },
                onOpenUsernameGeneratorClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                },
                onPasswordCheckerClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick,
                    )
                },
                onOpenPasswordGeneratorClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick,
                    )
                },
                onSetupTotpClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.SetupTotpClick)
                },
                onUriSettingsClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.UriSettingsClick)
                },
                onAddNewUriClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.AddNewUriClick)
                },
                onTooltipClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.TooltipClick)
                },
                onAddNewCustomFieldClick = {
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick,
                    )
                },
            )
        }
    }
}
