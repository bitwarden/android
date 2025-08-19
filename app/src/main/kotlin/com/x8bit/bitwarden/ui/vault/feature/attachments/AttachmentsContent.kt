package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.attachments.handlers.AttachmentsHandlers

/**
 * The top level content UI state for the [AttachmentsScreen] when viewing a content.
 */
@Suppress("LongMethod")
@Composable
fun AttachmentsContent(
    viewState: AttachmentsState.ViewState.Content,
    attachmentsHandlers: AttachmentsHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        if (viewState.attachments.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = BitwardenString.no_attachments),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("NoAttachmentsLabel")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
            }
            itemsIndexed(viewState.attachments) { index, it ->
                AttachmentListEntry(
                    attachmentItem = it,
                    onDeleteClick = attachmentsHandlers.onDeleteClick,
                    cardStyle = viewState.attachments.toListItemCardStyle(index = index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .testTag("AttachmentList"),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.add_new_attachment),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item {
            Text(
                text = viewState
                    .newAttachment
                    ?.displayName
                    ?: stringResource(id = BitwardenString.no_file_chosen),
                color = BitwardenTheme.colorScheme.text.secondary,
                style = BitwardenTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("SelectedFileNameLabel"),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.choose_file),
                onClick = attachmentsHandlers.onChooseFileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("AttachmentSelectFileButton"),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = BitwardenString.max_file_size),
                color = BitwardenTheme.colorScheme.text.secondary,
                style = BitwardenTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AttachmentListEntry(
    attachmentItem: AttachmentsState.AttachmentItem,
    onDeleteClick: (attachmentId: String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var shouldShowDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowDeleteDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.delete),
            message = stringResource(id = BitwardenString.do_you_really_want_to_delete),
            confirmButtonText = stringResource(id = BitwardenString.delete),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowDeleteDialog = false
                onDeleteClick(attachmentItem.id)
            },
            onDismissClick = { shouldShowDeleteDialog = false },
            onDismissRequest = { shouldShowDeleteDialog = false },
        )
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(cardStyle = cardStyle, paddingStart = 16.dp)
            .testTag("AttachmentRow"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = attachmentItem.title,
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("AttachmentNameLabel"),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = attachmentItem.displaySize,
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.labelSmall,
            modifier = Modifier
                .testTag("AttachmentSizeLabel"),
        )

        Spacer(modifier = Modifier.width(8.dp))

        BitwardenStandardIconButton(
            vectorIconRes = BitwardenDrawable.ic_trash,
            contentDescription = stringResource(id = BitwardenString.delete),
            onClick = { shouldShowDeleteDialog = true },
            modifier = Modifier
                .testTag("AttachmentDeleteButton"),
        )
    }
}
