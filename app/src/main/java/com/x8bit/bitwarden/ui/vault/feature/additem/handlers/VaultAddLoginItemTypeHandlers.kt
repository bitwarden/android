package com.x8bit.bitwarden.ui.vault.feature.additem.handlers

import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemAction
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemViewModel

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding login items to a vault.
 *
 * @property onUsernameTextChange Handles the action when the username text is changed.
 * @property onPasswordTextChange Handles the action when the password text is changed.
 * @property onUriTextChange Handles the action when the URI text is changed.
 * reprompt toggle is changed.
 * @property onOpenUsernameGeneratorClick Handles the action when the username generator
 * button is clicked.
 * @property onPasswordCheckerClick Handles the action when the password checker
 * button is clicked.
 * @property onOpenPasswordGeneratorClick Handles the action when the password generator
 * button is clicked.
 * @property onSetupTotpClick Handles the action when the setup TOTP button is clicked.
 * @property onUriSettingsClick Handles the action when the URI settings button is clicked.
 * @property onAddNewUriClick Handles the action when the add new URI button is clicked.
 */
@Suppress("LongParameterList")
class VaultAddLoginItemTypeHandlers(
    val onUsernameTextChange: (String) -> Unit,
    val onPasswordTextChange: (String) -> Unit,
    val onUriTextChange: (String) -> Unit,
    val onOpenUsernameGeneratorClick: () -> Unit,
    val onPasswordCheckerClick: () -> Unit,
    val onOpenPasswordGeneratorClick: () -> Unit,
    val onSetupTotpClick: (Boolean) -> Unit,
    val onCopyTotpKeyClick: (String) -> Unit,
    val onUriSettingsClick: () -> Unit,
    val onAddNewUriClick: () -> Unit,
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
                onSetupTotpClick = { isGranted ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.SetupTotpClick(isGranted),
                    )
                },
                onUriSettingsClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.UriSettingsClick)
                },
                onAddNewUriClick = {
                    viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.AddNewUriClick)
                },
                onCopyTotpKeyClick = { totpKey ->
                    viewModel.trySendAction(
                        VaultAddItemAction.ItemType.LoginType.CopyTotpKeyClick(
                            totpKey,
                        ),
                    )
                },
            )
        }
    }
}
