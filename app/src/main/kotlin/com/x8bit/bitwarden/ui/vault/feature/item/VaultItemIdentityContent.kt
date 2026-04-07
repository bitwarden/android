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
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
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
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultIdentityItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a Identity cipher.
 */
@Suppress("LongMethod", "MaxLineLength")
@Composable
fun VaultItemIdentityContent(
    identityState: VaultItemState.ViewState.Content.ItemType.Identity,
    commonState: VaultItemState.ViewState.Content.Common,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultIdentityItemTypeHandlers: VaultIdentityItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            isArchived = commonState.archived,
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "IdentityItemNameIcon",
            textFieldTestTag = "IdentityItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
        identityState.identityName?.let { identityName ->
            item(key = "identityName") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.identity_name),
                    value = identityName,
                    copyContentDescription = stringResource(id = BitwardenString.copy_identity_name),
                    textFieldTestTag = "IdentityNameEntry",
                    copyActionTestTag = "IdentityCopyNameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyIdentityNameClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = identityName),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.username?.let { username ->
            item(key = "username") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.username),
                    value = username,
                    copyContentDescription = stringResource(id = BitwardenString.copy_username),
                    textFieldTestTag = "IdentityUsernameEntry",
                    copyActionTestTag = "IdentityCopyUsernameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyUsernameClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = username),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.company?.let { company ->
            item(key = "company") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.company),
                    value = company,
                    copyContentDescription = stringResource(id = BitwardenString.copy_company),
                    textFieldTestTag = "IdentityCompanyEntry",
                    copyActionTestTag = "IdentityCopyCompanyButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyCompanyClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = company),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.ssn?.let { ssn ->
            item(key = "ssn") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.ssn),
                    value = ssn,
                    onValueChange = {},
                    readOnly = true,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(id = BitwardenString.copy_ssn),
                            onClick = vaultIdentityItemTypeHandlers.onCopySsnClick,
                            modifier = Modifier.testTag(tag = "IdentityCopySsnButton"),
                        )
                    },
                    passwordFieldTestTag = "IdentitySsnEntry",
                    showPasswordTestTag = "IdentityViewSsnButton",
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = ssn),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.passportNumber?.let { passportNumber ->
            item(key = "passportNumber") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.passport_number),
                    value = passportNumber,
                    copyContentDescription = stringResource(id = BitwardenString.copy_passport_number),
                    textFieldTestTag = "IdentityPassportNumberEntry",
                    copyActionTestTag = "IdentityCopyPassportNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPassportNumberClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = passportNumber),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.licenseNumber?.let { licenseNumber ->
            item(key = "licenseNumber") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.license_number),
                    value = licenseNumber,
                    copyContentDescription = stringResource(id = BitwardenString.copy_license_number),
                    textFieldTestTag = "IdentityLicenseNumberEntry",
                    copyActionTestTag = "IdentityCopyLicenseNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyLicenseNumberClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = licenseNumber),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.email?.let { email ->
            item(key = "email") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.email),
                    value = email,
                    copyContentDescription = stringResource(id = BitwardenString.copy_email),
                    textFieldTestTag = "IdentityEmailEntry",
                    copyActionTestTag = "IdentityCopyEmailButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyEmailClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = email),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.phone?.let { phone ->
            item(key = "phone") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.phone),
                    value = phone,
                    copyContentDescription = stringResource(id = BitwardenString.copy_phone),
                    textFieldTestTag = "IdentityPhoneEntry",
                    copyActionTestTag = "IdentityCopyPhoneButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPhoneClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = phone),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        identityState.address?.let { address ->
            item(key = "address") {
                IdentityCopyField(
                    label = stringResource(id = BitwardenString.address),
                    value = address,
                    copyContentDescription = stringResource(id = BitwardenString.copy_address),
                    textFieldTestTag = "IdentityAddressEntry",
                    copyActionTestTag = "IdentityCopyAddressButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyAddressClick,
                    cardStyle = identityState
                        .propertyList
                        .toListItemCardStyle(
                            index = identityState.propertyList.indexOf(element = address),
                            dividerPadding = 0.dp,
                        ),
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

@Composable
private fun IdentityCopyField(
    label: String,
    value: String,
    copyContentDescription: String,
    textFieldTestTag: String,
    copyActionTestTag: String,
    onCopyClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = label,
        value = value,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_copy,
                contentDescription = copyContentDescription,
                onClick = onCopyClick,
                modifier = Modifier.testTag(tag = copyActionTestTag),
            )
        },
        textFieldTestTag = textFieldTestTag,
        cardStyle = cardStyle,
        modifier = modifier,
    )
}
