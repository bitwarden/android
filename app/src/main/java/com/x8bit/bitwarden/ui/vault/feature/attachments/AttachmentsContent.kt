package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledTonalButton
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
                    text = stringResource(id = R.string.no_attachments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .semantics { testTag = "NoAttachmentsLabel" }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            items(viewState.attachments) {
                AttachmentListEntry(
                    attachmentItem = it,
                    onDeleteClick = attachmentsHandlers.onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.add_new_attachment),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = viewState
                    .newAttachment
                    ?.displayName
                    ?: stringResource(id = R.string.no_file_chosen),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenFilledTonalButton(
                label = stringResource(id = R.string.choose_file),
                onClick = attachmentsHandlers.onChooseFileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.max_file_size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AttachmentListEntry(
    attachmentItem: AttachmentsState.AttachmentItem,
    onDeleteClick: (attachmentId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowDeleteDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete),
            message = stringResource(id = R.string.do_you_really_want_to_delete),
            confirmButtonText = stringResource(id = R.string.delete),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                shouldShowDeleteDialog = false
                onDeleteClick(attachmentItem.id)
            },
            onDismissClick = { shouldShowDeleteDialog = false },
            onDismissRequest = { shouldShowDeleteDialog = false },
        )
    }

    Row(
        modifier = Modifier
            .bottomDivider(
                paddingStart = 16.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            .defaultMinSize(minHeight = 56.dp)
            .semantics { testTag = "AttachmentRow" }
            .padding(vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = attachmentItem.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = attachmentItem.displaySize,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier,
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = { shouldShowDeleteDialog = true },
            modifier = Modifier,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = stringResource(id = R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
