package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.component.CustomField
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a secure note cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemSecureNoteContent(
    commonState: VaultItemState.ViewState.Content.Common,
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
            iconTestTag = "SecureNoteItemNameIcon",
            textFieldTestTag = "SecureNoteItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
        )
        commonState.notes?.let { notes ->
            item {
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
                        .standardHorizontalMargin(),
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
            }

            items(customFields) { customField ->
                Spacer(modifier = Modifier.height(height = 8.dp))
                CustomField(
                    customField = customField,
                    onCopyCustomHiddenField =
                    vaultCommonItemTypeHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField =
                    vaultCommonItemTypeHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick =
                    vaultCommonItemTypeHandlers.onShowHiddenFieldClick,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        commonState.attachments.takeUnless { it?.isEmpty() == true }?.let { attachments ->
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.attachments),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            itemsIndexed(attachments) { index, attachmentItem ->
                AttachmentItemContent(
                    modifier = Modifier
                        .testTag("CipherAttachment")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                    attachmentItem = attachmentItem,
                    onAttachmentDownloadClick = vaultCommonItemTypeHandlers
                        .onAttachmentDownloadClick,
                    cardStyle = attachments.toListItemCardStyle(index = index),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp)
                    .semantics(mergeDescendants = true) { },
            ) {
                Text(
                    text = "${stringResource(id = R.string.date_updated)}: ",
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
                Text(
                    text = commonState.lastUpdated,
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
