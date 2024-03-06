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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a Identity cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemIdentityContent(
    identityState: VaultItemState.ViewState.Content.ItemType.Identity,
    commonState: VaultItemState.ViewState.Content.Common,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
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
                    .semantics { testTag = "ItemRow" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        identityState.identityName?.let { identityName ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.identity_name),
                    value = identityName,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.username?.let { username ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.username),
                    value = username,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.company?.let { company ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.company),
                    value = company,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.ssn?.let { ssn ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.ssn),
                    value = ssn,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.passportNumber?.let { passportNumber ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.passport_number),
                    value = passportNumber,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.licenseNumber?.let { licenseNumber ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.license_number),
                    value = licenseNumber,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.email?.let { email ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.email),
                    value = email,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.phone?.let { phone ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.phone),
                    value = phone,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        identityState.address?.let { address ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.address),
                    value = address,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
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
                BitwardenTextField(
                    label = stringResource(id = R.string.notes),
                    value = notes,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
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
                        .semantics { testTag = "CipherAttachment" }
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
