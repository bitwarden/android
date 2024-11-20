package com.x8bit.bitwarden.ui.tools.feature.send

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
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.tools.feature.send.handlers.SendHandlers
import kotlinx.collections.immutable.toImmutableList

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
            if (policyDisablesSend) {
                BitwardenInfoCalloutCard(
                    text = stringResource(id = R.string.send_disabled_warning),
                    modifier = Modifier
                        .testTag("SendOptionsPolicyInEffectLabel")
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.types),
                supportingLabel = SEND_TYPES_COUNT.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = R.string.type_text),
                supportingLabel = state.textTypeCount.toString(),
                startIcon = rememberVectorPainter(id = R.drawable.ic_file_text),
                onClick = sendHandlers.onTextTypeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SendTextFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = R.string.type_file),
                supportingLabel = state.fileTypeCount.toString(),
                startIcon = rememberVectorPainter(id = R.drawable.ic_file),
                onClick = sendHandlers.onFileTypeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SendFileFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.all_sends),
                supportingLabel = state.sendItems.size.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        items(state.sendItems) {
            SendListItem(
                startIcon = IconData.Local(it.type.iconRes),
                label = it.name,
                supportingLabel = it.deletionDate,
                trailingLabelIcons = it.iconList.toImmutableList(),
                showMoreOptions = !policyDisablesSend,
                onClick = { sendHandlers.onSendClick(it) },
                onCopyClick = { sendHandlers.onCopySendClick(it) },
                onEditClick = { sendHandlers.onEditSendClick(it) },
                onShareClick = { sendHandlers.onShareSendClick(it) },
                onDeleteClick = { sendHandlers.onDeleteSendClick(it) },
                onRemovePasswordClick = if (it.hasPassword) {
                    { sendHandlers.onRemovePasswordClick(it) }
                } else {
                    null
                },
                modifier = Modifier
                    .testTag("SendCell")
                    .padding(
                        start = 16.dp,
                        // There is some built-in padding to the menu button that makes up
                        // the visual difference here.
                        end = 12.dp,
                    )
                    .fillMaxWidth(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
