package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.tools.feature.send.handlers.SendHandlers

private const val SEND_TYPES_COUNT: Int = 2

/**
 * Content view for the [SendScreen].
 */
@Suppress("LongMethod")
@Composable
fun SendContent(
    policyDisablesSend: Boolean,
    state: SendState.ViewState.Content,
    sendHandlers: SendHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
        if (policyDisablesSend) {
            item {
                BitwardenInfoCalloutCard(
                    text = stringResource(id = BitwardenString.send_disabled_warning),
                    modifier = Modifier
                        .testTag("SendOptionsPolicyInEffectLabel")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.types),
                supportingLabel = SEND_TYPES_COUNT.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = BitwardenString.type_text),
                supportingLabel = state.textTypeCount.toString(),
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_file_text),
                onClick = sendHandlers.onTextTypeClick,
                cardStyle = CardStyle.Top(dividerPadding = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SendTextFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = BitwardenString.type_file),
                supportingLabel = state.fileTypeCount.toString(),
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_file),
                onClick = sendHandlers.onFileTypeClick,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SendFileFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.all_sends),
                supportingLabel = state.sendItems.size.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        itemsIndexed(state.sendItems) { index, it ->
            SendListItem(
                startIcon = IconData.Local(it.type.iconRes),
                label = it.name,
                supportingLabel = it.deletionDate,
                trailingLabelIcons = it.iconList,
                showMoreOptions = !policyDisablesSend,
                onClick = { sendHandlers.onSendClick(it) },
                onViewClick = { sendHandlers.onViewSendClick(it) },
                onCopyClick = { sendHandlers.onCopySendClick(it) },
                onEditClick = { sendHandlers.onEditSendClick(it) },
                onShareClick = { sendHandlers.onShareSendClick(it) },
                onDeleteClick = { sendHandlers.onDeleteSendClick(it) },
                onRemovePasswordClick = if (it.hasPassword) {
                    { sendHandlers.onRemovePasswordClick(it) }
                } else {
                    null
                },
                cardStyle = state
                    .sendItems
                    .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                modifier = Modifier
                    .testTag("SendCell")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
