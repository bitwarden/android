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
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultPassportItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a passport cipher.
 *
 * The canonical [VaultItemState.ViewState.Content.ItemType.Passport] state model splits the
 * holder name into [VaultItemState.ViewState.Content.ItemType.Passport.givenName] and
 * [VaultItemState.ViewState.Content.ItemType.Passport.surname], and splits the date of birth,
 * issue date, and expiration date into separate month/day/year strings. Each populated field is
 * rendered as a separate read-only row to mirror the data shape; combined-name or combined-date
 * formatting is left to the SDK and any future presentation layer.
 *
 * The passport number row is hidden by default and revealed via a toggle to mirror the national
 * identification number UX from the Bitwarden web client. Visibility is local to the composable
 * since the state model exposes `String?` only.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultItemPassportContent(
    commonState: VaultItemState.ViewState.Content.Common,
    passportState: VaultItemState.ViewState.Content.ItemType.Passport,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultPassportItemTypeHandlers: VaultPassportItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    var showPassportNumber by rememberSaveable { mutableStateOf(value = false) }
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
            iconTestTag = "PassportItemNameIcon",
            textFieldTestTag = "PassportItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        passportState.surname?.let { surname ->
            item(key = "surname") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.surname),
                    value = surname,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemSurnameEntry",
                    cardStyle = CardStyle.Top(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.givenName?.let { givenName ->
            item(key = "givenName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.given_name),
                    value = givenName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemGivenNameEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.passportNumber?.let { passportNumber ->
            item(key = "passportNumber") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.passport_number),
                    value = passportNumber,
                    onValueChange = {},
                    showPassword = showPassportNumber,
                    showPasswordChange = { showPassportNumber = !showPassportNumber },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_passport_number,
                            ),
                            onClick = vaultPassportItemTypeHandlers
                                .onCopyPassportNumberClick,
                            modifier = Modifier.testTag(
                                tag = "PassportCopyPassportNumberButton",
                            ),
                        )
                    },
                    passwordFieldTestTag = "PassportItemPassportNumberEntry",
                    showPasswordTestTag = "PassportViewPassportNumberButton",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.passportType?.let { passportType ->
            item(key = "passportType") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.passport_type),
                    value = passportType,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemPassportTypeEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.nationality?.let { nationality ->
            item(key = "nationality") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.nationality),
                    value = nationality,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemNationalityEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issuingCountry?.let { issuingCountry ->
            item(key = "issuingCountry") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_country),
                    value = issuingCountry,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssuingCountryEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issuingAuthority?.let { issuingAuthority ->
            item(key = "issuingAuthority") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_authority),
                    value = issuingAuthority,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssuingAuthorityEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.dobMonth?.let { dobMonth ->
            item(key = "dobMonth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.date_of_birth_month),
                    value = dobMonth,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemDateOfBirthMonthEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.dobDay?.let { dobDay ->
            item(key = "dobDay") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.date_of_birth_day),
                    value = dobDay,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemDateOfBirthDayEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.dobYear?.let { dobYear ->
            item(key = "dobYear") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.date_of_birth_year),
                    value = dobYear,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemDateOfBirthYearEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issueMonth?.let { issueMonth ->
            item(key = "issueMonth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issue_month),
                    value = issueMonth,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssueMonthEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issueDay?.let { issueDay ->
            item(key = "issueDay") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issue_day),
                    value = issueDay,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssueDayEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issueYear?.let { issueYear ->
            item(key = "issueYear") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issue_year),
                    value = issueYear,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemIssueYearEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.expirationMonth?.let { expirationMonth ->
            item(key = "expirationMonth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_month),
                    value = expirationMonth,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemExpirationMonthEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.expirationDay?.let { expirationDay ->
            item(key = "expirationDay") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_day),
                    value = expirationDay,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemExpirationDayEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.expirationYear?.let { expirationYear ->
            item(key = "expirationYear") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_year),
                    value = expirationYear,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "PassportItemExpirationYearEntry",
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
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
