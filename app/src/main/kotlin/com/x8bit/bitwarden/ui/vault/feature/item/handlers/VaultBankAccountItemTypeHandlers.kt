package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing bank
 * account items in a vault.
 */
data class VaultBankAccountItemTypeHandlers(
    val onCopyAccountNumberClick: () -> Unit,
    val onCopyRoutingNumberClick: () -> Unit,
    val onCopySwiftCodeClick: () -> Unit,
    val onCopyIbanClick: () -> Unit,
    val onAccountNumberVisibilityClick: (isVisible: Boolean) -> Unit,
    val onPinVisibilityClick: (isVisible: Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates the [VaultBankAccountItemTypeHandlers] using the [viewModel] to send
         * desired actions.
         */
        fun create(
            viewModel: VaultItemViewModel,
        ): VaultBankAccountItemTypeHandlers =
            VaultBankAccountItemTypeHandlers(
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
                onAccountNumberVisibilityClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount
                            .AccountNumberVisibilityClick(isVisible = it),
                    )
                },
                onPinVisibilityClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.BankAccount
                            .PinVisibilityClick(isVisible = it),
                    )
                },
            )
    }
}
