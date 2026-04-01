package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays the common attachment items for the vault item screen.
 *
 * @param attachments The attachments to display.
 * @param vaultCommonItemTypeHandlers Provides the handlers required for each attachment.
 */
fun LazyListScope.vaultItemAttachments(
    attachments: ImmutableList<VaultItemState.ViewState.Content.Common.AttachmentItem>,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
) {
    if (attachments.isEmpty()) return
    item(key = "attachmentsHeader") {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.attachments),
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
        VaultItemAttachment(
            attachmentItem = attachmentItem,
            onAttachmentDownloadClick = vaultCommonItemTypeHandlers.onAttachmentDownloadClick,
            onAttachmentPreviewClick = vaultCommonItemTypeHandlers.onAttachmentPreviewClick,
            onUpgradeToPremiumClick = vaultCommonItemTypeHandlers.onUpgradeToPremiumClick,
            cardStyle = attachments.toListItemCardStyle(index = index),
            modifier = Modifier
                .testTag(tag = "CipherAttachment")
                .fillMaxWidth()
                .standardHorizontalMargin()
                .animateItem(),
        )
    }
}
