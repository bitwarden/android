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
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textFieldTestTag = "ItemNameEntry",
            )
        }
        identityState.identityName?.let { identityName ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.identity_name),
                    value = identityName,
                    copyContentDescription = stringResource(id = R.string.copy_identity_name),
                    textFieldTestTag = "IdentityNameEntry",
                    copyActionTestTag = "IdentityCopyNameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyIdentityNameClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.username?.let { username ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.username),
                    value = username,
                    copyContentDescription = stringResource(id = R.string.copy_username),
                    textFieldTestTag = "IdentityUsernameEntry",
                    copyActionTestTag = "IdentityCopyUsernameButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyUsernameClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.company?.let { company ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.company),
                    value = company,
                    copyContentDescription = stringResource(id = R.string.copy_company),
                    textFieldTestTag = "IdentityCompanyEntry",
                    copyActionTestTag = "IdentityCopyCompanyButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyCompanyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.ssn?.let { ssn ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.ssn),
                    value = ssn,
                    copyContentDescription = stringResource(id = R.string.copy_ssn),
                    textFieldTestTag = "IdentitySsnEntry",
                    copyActionTestTag = "IdentityCopySsnButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopySsnClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.passportNumber?.let { passportNumber ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.passport_number),
                    value = passportNumber,
                    copyContentDescription = stringResource(id = R.string.copy_passport_number),
                    textFieldTestTag = "IdentityPassportNumberEntry",
                    copyActionTestTag = "IdentityCopyPassportNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPassportNumberClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.licenseNumber?.let { licenseNumber ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.license_number),
                    value = licenseNumber,
                    copyContentDescription = stringResource(id = R.string.copy_license_number),
                    textFieldTestTag = "IdentityLicenseNumberEntry",
                    copyActionTestTag = "IdentityCopyLicenseNumberButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyLicenseNumberClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.email?.let { email ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.email),
                    value = email,
                    copyContentDescription = stringResource(id = R.string.copy_email),
                    textFieldTestTag = "IdentityEmailEntry",
                    copyActionTestTag = "IdentityCopyEmailButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyEmailClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.phone?.let { phone ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.phone),
                    value = phone,
                    copyContentDescription = stringResource(id = R.string.copy_phone),
                    textFieldTestTag = "IdentityPhoneEntry",
                    copyActionTestTag = "IdentityCopyPhoneButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyPhoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.address?.let { address ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.address),
                    value = address,
                    copyContentDescription = stringResource(id = R.string.copy_address),
                    textFieldTestTag = "IdentityAddressEntry",
                    copyActionTestTag = "IdentityCopyAddressButton",
                    onCopyClick = vaultIdentityItemTypeHandlers.onCopyAddressClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCopyField(
                    label = stringResource(id = R.string.notes),
                    value = notes,
                    copyContentDescription = stringResource(id = R.string.copy_notes),
                    textFieldTestTag = "CipherNotesLabel",
                    copyActionTestTag = "CipherNotesCopyButton",
                    onCopyClick = vaultCommonItemTypeHandlers.onCopyNotesClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
    copyContentDescription: String,
    textFieldTestTag: String,
    copyActionTestTag: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextFieldWithActions(
        label = label,
        value = value,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_copy,
                contentDescription = copyContentDescription,
                onClick = onCopyClick,
                modifier = Modifier.testTag(tag = copyActionTestTag),
            )
        },
        modifier = modifier,
        textFieldTestTag = textFieldTestTag,
    )
}
