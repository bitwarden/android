package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing bank
 * account items in a vault.
 */
data class VaultBankAccountItemTypeHandlers(
    val onCopyNameOnAccountClick: () -> Unit,
    val onCopyAccountNumberClick: () -> Unit,
    val onCopyRoutingNumberClick: () -> Unit,
    val onCopyBranchNumberClick: () -> Unit,
    val onCopyPinClick: () -> Unit,
    val onCopySwiftCodeClick: () -> Unit,
    val onCopyIbanClick: () -> Unit,
    val onCopyBankContactPhoneClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates the [VaultBankAccountItemTypeHandlers] using the [viewModel] to send desired
         * actions.
         */
        fun create(viewModel: VaultItemViewModel): VaultBankAccountItemTypeHandlers =
            VaultBankAccountItemTypeHandlers(
                onCopyNameOnAccountClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyNameOnAccountClick,
                    )
                },
                onCopyAccountNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyAccountNumberClick,
                    )
                },
                onCopyRoutingNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyRoutingNumberClick,
                    )
                },
                onCopyBranchNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyBranchNumberClick,
                    )
                },
                onCopyPinClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyPinClick,
                    )
                },
                onCopySwiftCodeClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopySwiftCodeClick,
                    )
                },
                onCopyIbanClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyIbanClick,
                    )
                },
                onCopyBankContactPhoneClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount.CopyBankContactPhoneClick,
                    )
                },
            )
    }
}
