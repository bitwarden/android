package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel
import com.x8bit.bitwarden.ui.vault.model.VaultBankAccountType

/**
 * A collection of handler functions for managing user interactions on the Bank Account
 * portion of the Add/Edit cipher screen.
 *
 * @property onBankNameTextChange Handles changes to the bank name text input.
 * @property onNameOnAccountTextChange Handles changes to the name on account text input.
 * @property onAccountTypeSelect Handles selection of the account type.
 * @property onAccountNumberTextChange Handles changes to the account number text input.
 * @property onRoutingNumberTextChange Handles changes to the routing number text input.
 * @property onBranchNumberTextChange Handles changes to the branch number text input.
 * @property onPinTextChange Handles changes to the PIN text input.
 * @property onSwiftCodeTextChange Handles changes to the SWIFT code text input.
 * @property onIbanTextChange Handles changes to the IBAN text input.
 * @property onBankContactPhoneTextChange Handles changes to the bank contact phone text input.
 */
@Suppress("LongParameterList")
data class VaultAddEditBankAccountTypeHandlers(
    val onBankNameTextChange: (String) -> Unit,
    val onNameOnAccountTextChange: (String) -> Unit,
    val onAccountTypeSelect: (VaultBankAccountType) -> Unit,
    val onAccountNumberTextChange: (String) -> Unit,
    val onRoutingNumberTextChange: (String) -> Unit,
    val onBranchNumberTextChange: (String) -> Unit,
    val onPinTextChange: (String) -> Unit,
    val onSwiftCodeTextChange: (String) -> Unit,
    val onIbanTextChange: (String) -> Unit,
    val onBankContactPhoneTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditBankAccountTypeHandlers] by binding actions to the
         * provided [VaultAddEditViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditBankAccountTypeHandlers =
            VaultAddEditBankAccountTypeHandlers(
                onBankNameTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.BankNameTextChange(
                            bankName = it,
                        ),
                    )
                },
                onNameOnAccountTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.NameOnAccountTextChange(
                            nameOnAccount = it,
                        ),
                    )
                },
                onAccountTypeSelect = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.AccountTypeSelect(
                            accountType = it,
                        ),
                    )
                },
                onAccountNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.AccountNumberTextChange(
                            accountNumber = it,
                        ),
                    )
                },
                onRoutingNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.RoutingNumberTextChange(
                            routingNumber = it,
                        ),
                    )
                },
                onBranchNumberTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.BranchNumberTextChange(
                            branchNumber = it,
                        ),
                    )
                },
                onPinTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.PinTextChange(pin = it),
                    )
                },
                onSwiftCodeTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.SwiftCodeTextChange(
                            swiftCode = it,
                        ),
                    )
                },
                onIbanTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.IbanTextChange(iban = it),
                    )
                },
                onBankContactPhoneTextChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.BankAccountType.BankContactPhoneTextChange(
                            phone = it,
                        ),
                    )
                },
            )
    }
}
