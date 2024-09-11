package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem

/**
 * A collection of handler functions specifically tailored for managing actions
 * within the context of adding login items to a vault.
 *
 * @property onUsernameTextChange Handles the action when the username text is changed.
 * @property onPasswordTextChange Handles the action when the password text is changed.
 * @property onRemoveUriClick Handles the action when the URI is removed.
 * @property onUriValueChange Handles the action when the URI value is changed.
 * @property onOpenUsernameGeneratorClick Handles the action when the username generator
 * button is clicked.
 * @property onPasswordCheckerClick Handles the action when the password checker
 * button is clicked.
 * @property onOpenPasswordGeneratorClick Handles the action when the password generator
 * button is clicked.
 * @property onSetupTotpClick Handles the action when the setup TOTP button is clicked.
 * @property onCopyTotpKeyClick Handles the action when the copy TOTP text button is clicked.
 * @property onClearTotpKeyClick Handles the action when the clear TOTP text button is clicked.
 * @property onAddNewUriClick Handles the action when the add new URI button is clicked.
 * @property onPasswordVisibilityChange Handles the action when the password visibility button is
 * clicked.
 * @property onClearFido2CredentialClick Handles the action when the clear Fido2 credential button
 * is clicked.
 */
@Suppress("LongParameterList")
data class VaultAddEditLoginTypeHandlers(
    val onUsernameTextChange: (String) -> Unit,
    val onPasswordTextChange: (String) -> Unit,
    val onRemoveUriClick: (UriItem) -> Unit,
    val onUriValueChange: (UriItem) -> Unit,
    val onOpenUsernameGeneratorClick: () -> Unit,
    val onPasswordCheckerClick: () -> Unit,
    val onOpenPasswordGeneratorClick: () -> Unit,
    val onSetupTotpClick: (Boolean) -> Unit,
    val onCopyTotpKeyClick: (String) -> Unit,
    val onClearTotpKeyClick: () -> Unit,
    val onAddNewUriClick: () -> Unit,
    val onPasswordVisibilityChange: (Boolean) -> Unit,
    val onClearFido2CredentialClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditLoginTypeHandlers] by binding actions
         * to the provided [VaultAddEditViewModel].
         *
         * @param viewModel The [VaultAddEditViewModel] to which actions will be sent.
         * @return A fully initialized [VaultAddEditLoginTypeHandlers] object.
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditLoginTypeHandlers {
            return VaultAddEditLoginTypeHandlers(
                onUsernameTextChange = { newUsername ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.UsernameTextChange(newUsername),
                    )
                },
                onPasswordTextChange = { newPassword ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.PasswordTextChange(newPassword),
                    )
                },
                onUriValueChange = { newUri ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.UriValueChange(newUri),
                    )
                },
                onOpenUsernameGeneratorClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
                    )
                },
                onPasswordCheckerClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.PasswordCheckerClick,
                    )
                },
                onOpenPasswordGeneratorClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.OpenPasswordGeneratorClick,
                    )
                },
                onSetupTotpClick = { isGranted ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.SetupTotpClick(isGranted),
                    )
                },
                onRemoveUriClick = { uriItem ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.RemoveUriClick(
                            uriItem,
                        ),
                    )
                },
                onAddNewUriClick = {
                    viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.AddNewUriClick)
                },
                onCopyTotpKeyClick = { totpKey ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick(
                            totpKey,
                        ),
                    )
                },
                onClearTotpKeyClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.ClearTotpKeyClick,
                    )
                },
                onPasswordVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.PasswordVisibilityChange(it),
                    )
                },
                onClearFido2CredentialClick = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.LoginType.ClearFido2CredentialClick,
                    )
                },
            )
        }
    }
}
