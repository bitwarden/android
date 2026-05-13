package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditBankAccountTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultBankAccountType
import com.x8bit.bitwarden.ui.vault.util.longName
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a bank account cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditBankAccountItems(
    bankAccountState: VaultAddEditState.ViewState.Content.ItemType.BankAccount,
    bankAccountHandlers: VaultAddEditBankAccountTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.account_details),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.bank_name),
            value = bankAccountState.bankName,
            onValueChange = bankAccountHandlers.onBankNameTextChange,
            textFieldTestTag = "BankNameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.name_on_account),
            value = bankAccountState.nameOnAccount,
            onValueChange = bankAccountHandlers.onNameOnAccountTextChange,
            textFieldTestTag = "NameOnAccountEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        val resources = LocalResources.current
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.account_type),
            options = VaultBankAccountType
                .entries
                .map { it.longName() }
                .toImmutableList(),
            selectedOption = bankAccountState.accountType.longName(),
            onOptionSelected = { selectedString ->
                bankAccountHandlers.onAccountTypeSelect(
                    VaultBankAccountType
                        .entries
                        .first { it.longName.toString(resources) == selectedString },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("AccountTypePicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.account_number),
            value = bankAccountState.accountNumber,
            onValueChange = bankAccountHandlers.onAccountNumberTextChange,
            showPasswordTestTag = "ShowAccountNumberButton",
            passwordFieldTestTag = "AccountNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.routing_number),
            value = bankAccountState.routingNumber,
            onValueChange = bankAccountHandlers.onRoutingNumberTextChange,
            textFieldTestTag = "RoutingNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.branch_number),
            value = bankAccountState.branchNumber,
            onValueChange = bankAccountHandlers.onBranchNumberTextChange,
            textFieldTestTag = "BranchNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.pin),
            value = bankAccountState.pin,
            onValueChange = bankAccountHandlers.onPinTextChange,
            keyboardType = KeyboardType.NumberPassword,
            showPasswordTestTag = "ShowPinButton",
            passwordFieldTestTag = "PinEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.swift_code),
            value = bankAccountState.swiftCode,
            onValueChange = bankAccountHandlers.onSwiftCodeTextChange,
            textFieldTestTag = "SwiftCodeEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.iban),
            value = bankAccountState.iban,
            onValueChange = bankAccountHandlers.onIbanTextChange,
            showPasswordTestTag = "ShowIbanButton",
            passwordFieldTestTag = "IbanEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        BitwardenTextField(
            label = stringResource(id = BitwardenString.bank_contact_phone),
            value = bankAccountState.bankContactPhone,
            onValueChange = bankAccountHandlers.onBankContactPhoneTextChange,
            textFieldTestTag = "BankContactPhoneEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
