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
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultDriversLicenseItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a driver's license
 * cipher.
 */
@Suppress("LongMethod", "UnusedParameter")
@Composable
fun VaultItemDriversLicenseContent(
    commonState: VaultItemState.ViewState.Content.Common,
    driversLicenseState: VaultItemState.ViewState.Content.ItemType.DriversLicense,
    vaultDriversLicenseItemTypeHandlers: VaultDriversLicenseItemTypeHandlers,
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
            iconTestTag = "DriversLicenseItemNameIcon",
            textFieldTestTag = "DriversLicenseItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )

        driversLicenseState.fullName?.let { fullName ->
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
                        .testTag("DriversLicenseItemFullNameEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("DriversLicenseItemLicenseNumberEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("DriversLicenseItemIssuingCountryEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("DriversLicenseItemIssuingStateEntry")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        driversLicenseState.expiration?.let { expiration ->
            item(key = "expiration") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration_date),
                    value = expiration,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("DriversLicenseItemExpirationEntry")
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
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
                        .testTag("DriversLicenseItemLicenseClassEntry")
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
