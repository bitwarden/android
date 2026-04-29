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
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultDriversLicenseItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a driver's license
 * cipher.
 *
 * The canonical [VaultItemState.ViewState.Content.ItemType.DriversLicense] state model splits
 * the holder name into first/middle/last and the expiration into month/day/year. Each populated
 * field is rendered as a separate read-only row to mirror the data shape; combined-name or
 * combined-date formatting is left to the SDK and any future presentation layer.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemDriversLicenseContent(
    commonState: VaultItemState.ViewState.Content.Common,
    driversLicenseState: VaultItemState.ViewState.Content.ItemType.DriversLicense,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultDriversLicenseItemTypeHandlers: VaultDriversLicenseItemTypeHandlers,
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
            iconTestTag = "DriversLicenseItemNameIcon",
            textFieldTestTag = "DriversLicenseItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        driversLicenseState.firstName?.let { firstName ->
            item(key = "firstName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.first_name),
                    value = firstName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemFirstNameEntry",
                    cardStyle = CardStyle.Top(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.middleName?.let { middleName ->
            item(key = "middleName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.middle_name),
                    value = middleName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemMiddleNameEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.lastName?.let { lastName ->
            item(key = "lastName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.last_name),
                    value = lastName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemLastNameEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.licenseNumber?.let { licenseNumber ->
            item(key = "licenseNumber") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.license_number),
                    value = licenseNumber,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_license_number,
                            ),
                            onClick = vaultDriversLicenseItemTypeHandlers
                                .onCopyLicenseNumberClick,
                            modifier = Modifier.testTag(
                                tag = "DriversLicenseCopyLicenseNumberButton",
                            ),
                        )
                    },
                    textFieldTestTag = "DriversLicenseItemLicenseNumberEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.issuingCountry?.let { issuingCountry ->
            item(key = "issuingCountry") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_country),
                    value = issuingCountry,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemIssuingCountryEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.issuingState?.let { issuingState ->
            item(key = "issuingState") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.issuing_state),
                    value = issuingState,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemIssuingStateEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.expirationMonth?.let { expirationMonth ->
            item(key = "expirationMonth") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_month),
                    value = expirationMonth,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemExpirationMonthEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.expirationDay?.let { expirationDay ->
            item(key = "expirationDay") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_day),
                    value = expirationDay,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemExpirationDayEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.expirationYear?.let { expirationYear ->
            item(key = "expirationYear") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_year),
                    value = expirationYear,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemExpirationYearEntry",
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.licenseClass?.let { licenseClass ->
            item(key = "licenseClass") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.license_class),
                    value = licenseClass,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "DriversLicenseItemLicenseClassEntry",
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
