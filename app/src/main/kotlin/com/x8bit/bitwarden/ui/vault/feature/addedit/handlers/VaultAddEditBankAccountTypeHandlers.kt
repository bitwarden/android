package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultBankAccountType

/**
 * Provides a set of handlers for interactions related to bank account types within the vault
 * add/edit screen.
 */
@Suppress("LongParameterList")
data class VaultAddEditBankAccountTypeHandlers(
    val onBankNameTextChange: (String) -> Unit,
    val onNameOnAccountTextChange: (String) -> Unit,
    val onAccountTypeSelect: (VaultBankAccountType) -> Unit,
    val onAccountNumberTextChange: (String) -> Unit,
    val onAccountNumberVisibilityChange: (Boolean) -> Unit,
    val onRoutingNumberTextChange: (String) -> Unit,
    val onBranchNumberTextChange: (String) -> Unit,
    val onPinTextChange: (String) -> Unit,
    val onPinVisibilityChange: (Boolean) -> Unit,
    val onSwiftCodeTextChange: (String) -> Unit,
    val onIbanTextChange: (String) -> Unit,
    val onBankContactPhoneTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultAddEditBankAccountTypeHandlers] with handlers that
         * dispatch actions to the provided ViewModel.
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditBankAccountTypeHandlers =
            VaultAddEditBankAccountTypeHandlers(
                onBankNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .BankNameTextChange(bankName = it),
                    )
                },
                onNameOnAccountTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .NameOnAccountTextChange(nameOnAccount = it),
                    )
                },
                onAccountTypeSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .AccountTypeSelect(accountType = it),
                    )
                },
                onAccountNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .AccountNumberTextChange(accountNumber = it),
                    )
                },
                onAccountNumberVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .AccountNumberVisibilityChange(isVisible = it),
                    )
                },
                onRoutingNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .RoutingNumberTextChange(routingNumber = it),
                    )
                },
                onBranchNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .BranchNumberTextChange(branchNumber = it),
                    )
                },
                onPinTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .PinTextChange(pin = it),
                    )
                },
                onPinVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .PinVisibilityChange(isVisible = it),
                    )
                },
                onSwiftCodeTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .SwiftCodeTextChange(swiftCode = it),
                    )
                },
                onIbanTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .IbanTextChange(iban = it),
                    )
                },
                onBankContactPhoneTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType
                            .BankContactPhoneTextChange(phone = it),
                    )
                },
            )
    }
}
