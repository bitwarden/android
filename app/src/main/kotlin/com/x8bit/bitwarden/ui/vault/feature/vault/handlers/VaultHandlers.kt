package com.x8bit.bitwarden.ui.vault.feature.vault.handlers

import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultAction
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultViewModel
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

/**
 * A collection of handler functions for managing actions within the context of the vault screen.
 */
data class VaultHandlers(
    val vaultFilterTypeSelect: (VaultFilterType) -> Unit,
    val selectAddItemTypeClickAction: () -> Unit,
    val addItemClickAction: (CreateVaultItemType) -> Unit,
    val searchIconClickAction: () -> Unit,
    val accountLockClickAction: (AccountSummary) -> Unit,
    val accountLogoutClickAction: (AccountSummary) -> Unit,
    val accountSwitchClickAction: (AccountSummary) -> Unit,
    val addAccountClickAction: () -> Unit,
    val syncAction: () -> Unit,
    val lockAction: () -> Unit,
    val vaultItemClick: (VaultState.ViewState.VaultItem) -> Unit,
    val folderClick: (VaultState.ViewState.FolderItem) -> Unit,
    val collectionClick: (VaultState.ViewState.CollectionItem) -> Unit,
    val verificationCodesClick: () -> Unit,
    val loginGroupClick: () -> Unit,
    val cardGroupClick: () -> Unit,
    val identityGroupClick: () -> Unit,
    val secureNoteGroupClick: () -> Unit,
    val sshKeyGroupClick: () -> Unit,
    val trashClick: () -> Unit,
    val tryAgainClick: () -> Unit,
    val dialogDismiss: () -> Unit,
    val overflowOptionClick: (ListingItemOverflowAction.VaultAction) -> Unit,
    val overflowMasterPasswordRepromptSubmit: (
        ListingItemOverflowAction.VaultAction,
        String,
    ) -> Unit,
    val masterPasswordRepromptSubmit: (VaultState.ViewState.VaultItem, String) -> Unit,
    val dismissImportActionCard: () -> Unit,
    val importActionCardClick: () -> Unit,
    val flightRecorderGoToSettingsClick: () -> Unit,
    val dismissFlightRecorderSnackbar: () -> Unit,
    val onShareCipherDecryptionErrorClick: (selectedCipherId: String) -> Unit,
    val onShareAllCipherDecryptionErrorsClick: () -> Unit,
    val onKdfUpdatePasswordRepromptSubmit: (password: String) -> Unit,
    val onEnabledThirdPartyAutofillClick: () -> Unit,
    val onDismissThirdPartyAutofillDialogClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultHandlers] by binding actions to the provided
         * [VaultViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultViewModel): VaultHandlers =
            VaultHandlers(
                vaultFilterTypeSelect = {
                    viewModel.trySendAction(VaultAction.VaultFilterTypeSelect(it))
                },
                selectAddItemTypeClickAction = {
                    viewModel.trySendAction(VaultAction.SelectAddItemType)
                },
                addItemClickAction = { viewModel.trySendAction(VaultAction.AddItemClick(it)) },
                searchIconClickAction = { viewModel.trySendAction(VaultAction.SearchIconClick) },
                accountLockClickAction = {
                    viewModel.trySendAction(VaultAction.LockAccountClick(it))
                },
                accountLogoutClickAction = {
                    viewModel.trySendAction(VaultAction.LogoutAccountClick(it))
                },
                accountSwitchClickAction = {
                    viewModel.trySendAction(VaultAction.SwitchAccountClick(it))
                },
                addAccountClickAction = { viewModel.trySendAction(VaultAction.AddAccountClick) },
                syncAction = { viewModel.trySendAction(VaultAction.SyncClick) },
                lockAction = { viewModel.trySendAction(VaultAction.LockClick) },
                vaultItemClick = { viewModel.trySendAction(VaultAction.VaultItemClick(it)) },
                folderClick = { viewModel.trySendAction(VaultAction.FolderClick(it)) },
                collectionClick = { viewModel.trySendAction(VaultAction.CollectionClick(it)) },
                verificationCodesClick = {
                    viewModel.trySendAction(VaultAction.VerificationCodesClick)
                },
                loginGroupClick = { viewModel.trySendAction(VaultAction.LoginGroupClick) },
                cardGroupClick = { viewModel.trySendAction(VaultAction.CardGroupClick) },
                identityGroupClick = { viewModel.trySendAction(VaultAction.IdentityGroupClick) },
                secureNoteGroupClick = {
                    viewModel.trySendAction(VaultAction.SecureNoteGroupClick)
                },
                sshKeyGroupClick = { viewModel.trySendAction(VaultAction.SshKeyGroupClick) },
                trashClick = { viewModel.trySendAction(VaultAction.TrashClick) },
                tryAgainClick = { viewModel.trySendAction(VaultAction.TryAgainClick) },
                dialogDismiss = { viewModel.trySendAction(VaultAction.DialogDismiss) },
                overflowOptionClick = {
                    viewModel.trySendAction(VaultAction.OverflowOptionClick(it))
                },
                overflowMasterPasswordRepromptSubmit = { action, password ->
                    viewModel.trySendAction(
                        VaultAction.OverflowMasterPasswordRepromptSubmit(
                            overflowAction = action,
                            password = password,
                        ),
                    )
                },
                masterPasswordRepromptSubmit = { item, password ->
                    viewModel.trySendAction(
                        VaultAction.MasterPasswordRepromptSubmit(
                            item = item,
                            password = password,
                        ),
                    )
                },
                dismissImportActionCard = {
                    viewModel.trySendAction(VaultAction.DismissImportActionCard)
                },
                importActionCardClick = {
                    viewModel.trySendAction(VaultAction.ImportActionCardClick)
                },
                flightRecorderGoToSettingsClick = {
                    viewModel.trySendAction(VaultAction.FlightRecorderGoToSettingsClick)
                },
                dismissFlightRecorderSnackbar = {
                    viewModel.trySendAction(VaultAction.DismissFlightRecorderSnackbar)
                },
                onShareCipherDecryptionErrorClick = {
                    viewModel.trySendAction(VaultAction.ShareCipherDecryptionErrorClick(it))
                },
                onShareAllCipherDecryptionErrorsClick = {
                    viewModel.trySendAction(VaultAction.ShareAllCipherDecryptionErrorsClick)
                },
                onEnabledThirdPartyAutofillClick = {
                    viewModel.trySendAction(VaultAction.EnableThirdPartyAutofillClick)
                },
                onDismissThirdPartyAutofillDialogClick = {
                    viewModel.trySendAction(VaultAction.DismissThirdPartyAutofillDialogClick)
                },
                onKdfUpdatePasswordRepromptSubmit = {
                    viewModel.trySendAction(VaultAction.KdfUpdatePasswordRepromptSubmit(it))
                },
            )
    }
}
