package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTonalIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
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
    LazyColumn(modifier = modifier) {
        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.item_information),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.name),
                value = commonState.name,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = Modifier
                    .testTag("ItemNameEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        identityState.identityName?.let { identityName ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.identity_name),
                    value = identityName,
                    contentDescription = stringResource(id = R.string.copy_identity_name),
                    textFieldTestTag = "IdentityNameEntry",
                    copyActionTestTag = "IdentityCopyNameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyIdentityNameClick,
                )
            }
        }
        identityState.username?.let { username ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.username),
                    value = username,
                    contentDescription = stringResource(id = R.string.copy_username),
                    textFieldTestTag = "IdentityUsernameEntry",
                    copyActionTestTag = "IdentityCopyUsernameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyUsernameClick,
                )
            }
        }
        identityState.company?.let { company ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.company),
                    value = company,
                    contentDescription = stringResource(id = R.string.copy_company),
                    textFieldTestTag = "IdentityCompanyEntry",
                    copyActionTestTag = "IdentityCopyCompanyButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyCompanyClick,
                )
            }
        }
        identityState.ssn?.let { ssn ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.ssn),
                    value = ssn,
                    contentDescription = stringResource(id = R.string.copy_ssn),
                    textFieldTestTag = "IdentitySsnEntry",
                    copyActionTestTag = "IdentityCopySsnButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopySsnClick,
                )
            }
        }
        identityState.passportNumber?.let { passportNumber ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.passport_number),
                    value = passportNumber,
                    contentDescription = stringResource(id = R.string.copy_passport_number),
                    textFieldTestTag = "IdentityPassportNumberEntry",
                    copyActionTestTag = "IdentityCopyPassportNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPassportNumberClick,
                )
            }
        }
        identityState.licenseNumber?.let { licenseNumber ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.license_number),
                    value = licenseNumber,
                    contentDescription = stringResource(id = R.string.copy_license_number),
                    textFieldTestTag = "IdentityLicenseNumberEntry",
                    copyActionTestTag = "IdentityCopyLicenseNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyLicenseNumberClick,
                )
            }
        }
        identityState.email?.let { email ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.email),
                    value = email,
                    contentDescription = stringResource(id = R.string.copy_email),
                    textFieldTestTag = "IdentityEmailEntry",
                    copyActionTestTag = "IdentityCopyEmailButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyEmailClick,
                )
            }
        }
        identityState.phone?.let { phone ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.phone),
                    value = phone,
                    contentDescription = stringResource(id = R.string.copy_phone),
                    textFieldTestTag = "IdentityPhoneEntry",
                    copyActionTestTag = "IdentityCopyPhoneButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPhoneClick,
                )
            }
        }
        identityState.address?.let { address ->
            item {
                IdentityCopyField(
                    label = stringResource(id = R.string.address),
                    value = address,
                    contentDescription = stringResource(id = R.string.copy_address),
                    textFieldTestTag = "IdentityAddressEntry",
                    copyActionTestTag = "IdentityCopyAddressButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyAddressClick,
                )
            }
        }
        commonState.notes?.let { notes ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.notes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                IdentityCopyField(
                    label = stringResource(id = R.string.notes),
                    value = notes,
                    contentDescription = stringResource(id = R.string.copy_notes),
                    textFieldTestTag = "CipherNotesLabel",
                    copyActionTestTag = "CipherNotesCopyButton",
                    onCopyClick = vaultCommonItemTypeHandlers.onCopyNotesClick,
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            items(customFields) { customField ->
                Spacer(modifier = Modifier.height(8.dp))
                CustomField(
                    customField = customField,
                    onCopyCustomHiddenField = vaultCommonItemTypeHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField = vaultCommonItemTypeHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick = vaultCommonItemTypeHandlers.onShowHiddenFieldClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        commonState.attachments.takeUnless { it?.isEmpty() == true }?.let { attachments ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.attachments),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            items(attachments) { attachmentItem ->
                AttachmentItemContent(
                    modifier = Modifier
                        .testTag("CipherAttachment")
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    attachmentItem = attachmentItem,
                    onAttachmentDownloadClick =
                    vaultCommonItemTypeHandlers.onAttachmentDownloadClick,
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            VaultItemUpdateText(
                header = "${stringResource(id = R.string.date_updated)}: ",
                text = commonState.lastUpdated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
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
    contentDescription: String,
    textFieldTestTag: String,
    copyActionTestTag: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextFieldWithActions(
        label = label,
        value = value,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_copy,
                contentDescription = contentDescription,
                onClick = onCopyClick,
                modifier = Modifier.testTag(tag = copyActionTestTag),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textFieldTestTag = textFieldTestTag,
    )
}
