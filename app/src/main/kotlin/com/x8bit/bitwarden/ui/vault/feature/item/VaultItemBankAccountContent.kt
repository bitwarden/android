package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemAttachments
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemCustomFields
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemHistory
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemNotes
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultBankAccountItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a bank account cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemBankAccountContent(
    commonState: VaultItemState.ViewState.Content.Common,
    bankAccountState: VaultItemState.ViewState.Content.ItemType.BankAccount,
    vaultBankAccountItemTypeHandlers: VaultBankAccountItemTypeHandlers,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            isArchived = commonState.archived,
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "BankAccountItemNameIcon",
            textFieldTestTag = "BankAccountItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )

        bankAccountState.bankName?.let { bankName ->
            item(key = "bankName") {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.bank_name),
                    value = bankName,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Top(),
                    modifier = Modifier
                        .testTag("BankAccountItemBankNameEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.nameOnAccount?.let { nameOnAccount ->
            item(key = "nameOnAccount") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.name_on_account),
                    value = nameOnAccount,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemNameOnAccountEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.accountType?.let { accountType ->
            item(key = "accountType") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.account_type),
                    value = accountType,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemAccountTypeEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.accountNumber?.let { accountNumberData ->
            item(key = "accountNumber") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.account_number),
                    value = accountNumberData.number,
                    onValueChange = { },
                    singleLine = false,
                    readOnly = true,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_account_number,
                            ),
                            onClick =
                                vaultBankAccountItemTypeHandlers.onCopyAccountNumberClick,
                            modifier = Modifier.testTag(
                                tag = "BankAccountCopyAccountNumberButton",
                            ),
                        )
                    },
                    showPassword = accountNumberData.isVisible,
                    showPasswordTestTag = "ShowAccountNumberButton",
                    showPasswordChange =
                        vaultBankAccountItemTypeHandlers.onAccountNumberVisibilityClick,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemAccountNumberEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.routingNumber?.let { routingNumber ->
            item(key = "routingNumber") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.routing_number),
                    value = routingNumber,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_routing_number,
                            ),
                            onClick =
                                vaultBankAccountItemTypeHandlers.onCopyRoutingNumberClick,
                            modifier = Modifier.testTag(
                                tag = "BankAccountCopyRoutingNumberButton",
                            ),
                        )
                    },
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemRoutingNumberEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.branchNumber?.let { branchNumber ->
            item(key = "branchNumber") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.branch_number),
                    value = branchNumber,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemBranchNumberEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.pin?.let { pinData ->
            item(key = "pin") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.pin),
                    value = pinData.pin,
                    onValueChange = { },
                    singleLine = false,
                    readOnly = true,
                    showPassword = pinData.isVisible,
                    showPasswordTestTag = "ShowPinButton",
                    showPasswordChange =
                        vaultBankAccountItemTypeHandlers.onPinVisibilityClick,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemPinEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.swiftCode?.let { swiftCode ->
            item(key = "swiftCode") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.swift_code),
                    value = swiftCode,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_swift_code,
                            ),
                            onClick =
                                vaultBankAccountItemTypeHandlers.onCopySwiftCodeClick,
                            modifier = Modifier.testTag(
                                tag = "BankAccountCopySwiftCodeButton",
                            ),
                        )
                    },
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemSwiftCodeEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.iban?.let { iban ->
            item(key = "iban") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.iban),
                    value = iban,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_iban,
                            ),
                            onClick = vaultBankAccountItemTypeHandlers.onCopyIbanClick,
                            modifier = Modifier.testTag(
                                tag = "BankAccountCopyIbanButton",
                            ),
                        )
                    },
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("BankAccountItemIbanEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        bankAccountState.bankContactPhone?.let { phone ->
            item(key = "bankContactPhone") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.bank_contact_phone),
                    value = phone,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
                        .testTag("BankAccountItemBankContactPhoneEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        vaultItemNotes(
            notes = commonState.notes,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemCustomFields(
            customFields = commonState.customFields,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemAttachments(
            attachments = commonState.attachments,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemHistory(
            commonState = commonState,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
            loginPasswordRevisionDate = null,
        )

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
