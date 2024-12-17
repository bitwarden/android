package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Attachment UI common for all item types.
 */
@Suppress("LongMethod")
@Composable
fun AttachmentItemContent(
    attachmentItem: VaultItemState.ViewState.Content.Common.AttachmentItem,
    onAttachmentDownloadClick: (VaultItemState.ViewState.Content.Common.AttachmentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowPremiumWarningDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowSizeWarningDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .bottomDivider()
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 8.dp)
            .testTag("CipherAttachment"),
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
            vectorIconRes = R.drawable.ic_download,
            contentDescription = stringResource(id = R.string.download),
            onClick = {
                if (!attachmentItem.isDownloadAllowed) {
                    shouldShowPremiumWarningDialog = true
                    return@BitwardenStandardIconButton
                }

                if (attachmentItem.isLargeFile) {
                    shouldShowSizeWarningDialog = true
                    return@BitwardenStandardIconButton
                }

                onAttachmentDownloadClick(attachmentItem)
            },
            modifier = Modifier
                .testTag("AttachmentDownloadButton"),
        )
    }

    if (shouldShowPremiumWarningDialog) {
        AlertDialog(
            onDismissRequest = { shouldShowPremiumWarningDialog = false },
            confirmButton = {
                BitwardenTextButton(
                    label = stringResource(R.string.ok),
                    onClick = { shouldShowPremiumWarningDialog = false },
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.premium_required),
                    style = BitwardenTheme.typography.bodyMedium,
                )
            },
            containerColor = BitwardenTheme.colorScheme.background.primary,
            iconContentColor = BitwardenTheme.colorScheme.icon.secondary,
            titleContentColor = BitwardenTheme.colorScheme.text.primary,
            textContentColor = BitwardenTheme.colorScheme.text.primary,
        )
    }

    if (shouldShowSizeWarningDialog) {
        BitwardenTwoButtonDialog(
            title = null,
            message = stringResource(R.string.attachment_large_warning, attachmentItem.displaySize),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirmClick = {
                shouldShowSizeWarningDialog = false
                onAttachmentDownloadClick(attachmentItem)
            },
            onDismissClick = { shouldShowSizeWarningDialog = false },
            onDismissRequest = { shouldShowSizeWarningDialog = false },
        )
    }
}
