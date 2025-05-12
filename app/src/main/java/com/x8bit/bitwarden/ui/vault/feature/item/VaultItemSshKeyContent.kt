package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenHyperTextLink
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.component.CustomField
import com.x8bit.bitwarden.ui.vault.feature.item.component.VaultItemUpdateText
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultSshKeyItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a SSH key cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemSshKeyContent(
    commonState: VaultItemState.ViewState.Content.Common,
    sshKeyItemState: VaultItemState.ViewState.Content.ItemType.SshKey,
    vaultSshKeyItemTypeHandlers: VaultSshKeyItemTypeHandlers,
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
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "SshKeyItemNameIcon",
            textFieldTestTag = "SshKeyItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        item(key = "publicKey") {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.public_key),
                value = sshKeyItemState.publicKey,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_copy,
                        contentDescription = stringResource(id = R.string.copy_public_key),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyPublicKeyClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyPublicKeyButton"),
                    )
                },
                cardStyle = CardStyle.Top(),
                modifier = Modifier
                    .testTag("SshKeyItemPublicKeyEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        item(key = "privateKey") {
            BitwardenPasswordField(
                label = stringResource(id = R.string.private_key),
                value = sshKeyItemState.privateKey,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_copy,
                        contentDescription = stringResource(id = R.string.copy_private_key),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyPrivateKeyClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyPrivateKeyButton"),
                    )
                },
                showPassword = sshKeyItemState.showPrivateKey,
                showPasswordTestTag = "ViewPrivateKeyButton",
                showPasswordChange = vaultSshKeyItemTypeHandlers.onShowPrivateKeyClick,
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("SshKeyItemPrivateKeyEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        item(key = "fingerprint") {
            BitwardenTextField(
                label = stringResource(id = R.string.fingerprint),
                value = sshKeyItemState.fingerprint,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_copy,
                        contentDescription = stringResource(id = R.string.copy_fingerprint),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyFingerprintClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyFingerprintButton"),
                    )
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag("SshKeyItemFingerprintEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        commonState.notes?.let { notes ->
            item(key = "notes") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.additional_options),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.notes),
                    value = notes,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = R.drawable.ic_copy,
                            contentDescription = stringResource(id = R.string.copy_notes),
                            onClick = vaultCommonItemTypeHandlers.onCopyNotesClick,
                            modifier = Modifier.testTag(tag = "CipherNotesCopyButton"),
                        )
                    },
                    textFieldTestTag = "CipherNotesLabel",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item(key = "customFieldsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
            itemsIndexed(
                items = customFields,
                key = { index, _ -> "customField_$index" },
            ) { _, customField ->
                Spacer(modifier = Modifier.height(height = 8.dp))
                CustomField(
                    customField = customField,
                    onCopyCustomHiddenField = vaultCommonItemTypeHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField = vaultCommonItemTypeHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick = vaultCommonItemTypeHandlers.onShowHiddenFieldClick,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        commonState.attachments.takeUnless { it?.isEmpty() == true }?.let { attachments ->
            item(key = "attachmentsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.attachments),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            itemsIndexed(
                items = attachments,
                key = { index, _ -> "attachment_$index" },
            ) { index, attachmentItem ->
                AttachmentItemContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                    attachmentItem = attachmentItem,
                    onAttachmentDownloadClick = vaultCommonItemTypeHandlers
                        .onAttachmentDownloadClick,
                    cardStyle = attachments.toListItemCardStyle(index = index),
                )
            }
        }

        item(key = "lastUpdated") {
            Spacer(modifier = Modifier.height(height = 16.dp))
            VaultItemUpdateText(
                header = "${stringResource(id = R.string.date_updated)}: ",
                text = commonState.lastUpdated,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp)
                    .animateItem()
                    .testTag("SshKeyItemLastUpdated"),
            )
        }

        commonState.passwordHistoryCount?.let { passwordHistoryCount ->
            item(key = "passwordHistoryCount") {
                Spacer(modifier = Modifier.height(height = 4.dp))
                BitwardenHyperTextLink(
                    annotatedResId = R.string.password_history_count,
                    args = arrayOf(passwordHistoryCount.toString()),
                    annotationKey = "passwordHistory",
                    accessibilityString = stringResource(id = R.string.password_history),
                    onClick = vaultCommonItemTypeHandlers.onPasswordHistoryClick,
                    style = BitwardenTheme.typography.labelMedium,
                    modifier = Modifier
                        .wrapContentWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 12.dp)
                        .animateItem(),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
