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
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
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
 */
@Suppress("LongMethod", "UnusedParameter")
@Composable
fun VaultItemPassportContent(
    commonState: VaultItemState.ViewState.Content.Common,
    passportState: VaultItemState.ViewState.Content.ItemType.Passport,
    vaultPassportItemTypeHandlers: VaultPassportItemTypeHandlers,
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
            iconTestTag = "PassportItemNameIcon",
            textFieldTestTag = "PassportItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )

        passportState.fullName?.let { fullName ->
            item(key = "fullName") {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.name),
                    value = fullName,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Top(),
                    modifier = Modifier
                        .testTag("PassportItemFullNameEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.dateOfBirth?.let { dateOfBirth ->
            item(key = "dateOfBirth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.date_of_birth),
                    value = dateOfBirth,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemDateOfBirthEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemNationalityEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.passportNumber?.let { passportNumber ->
            item(key = "passportNumber") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.passport_number),
                    value = passportNumber,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemPassportNumberEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemPassportTypeEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemIssuingCountryEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemIssuingAuthorityEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.issueDate?.let { issueDate ->
            item(key = "issueDate") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issue_date),
                    value = issueDate,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("PassportItemIssueDateEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        passportState.expirationDate?.let { expirationDate ->
            item(key = "expirationDate") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_date),
                    value = expirationDate,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
                        .testTag("PassportItemExpirationDateEntry")
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
