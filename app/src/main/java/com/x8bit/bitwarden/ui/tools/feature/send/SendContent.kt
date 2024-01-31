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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderTextWithSupportLabel
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.tools.feature.send.handlers.SendHandlers

/**
 * Content view for the [SendScreen].
 */
@Suppress("LongMethod")
@Composable
fun SendContent(
    state: SendState.ViewState.Content,
    sendHandlers: SendHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.types),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = R.string.type_text),
                supportingLabel = state.textTypeCount.toString(),
                startIcon = painterResource(id = R.drawable.ic_send_text),
                onClick = sendHandlers.onTextTypeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                label = stringResource(id = R.string.type_file),
                supportingLabel = state.fileTypeCount.toString(),
                startIcon = painterResource(id = R.drawable.ic_send_file),
                onClick = sendHandlers.onFileTypeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenListHeaderTextWithSupportLabel(
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
                trailingLabelIcons = it.iconList,
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
                    .semantics { testTag = "SendCell" }
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
