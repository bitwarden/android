package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenEmptyContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the flight recorder recorded logs screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordedLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordedLogsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel) { event ->
        when (event) {
            RecordedLogsEvent.NavigateBack -> onNavigateBack()
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.recorded_logs_title),
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(RecordedLogsAction.BackClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    RecordedLogsOverflowMenu(
                        isOverflowEnabled = state.viewState.isOverflowEnabled,
                        onDeleteAllClick = remember(viewModel) {
                            { viewModel.trySendAction(RecordedLogsAction.DeleteAllClick) }
                        },
                        onShareAllClick = remember(viewModel) {
                            { viewModel.trySendAction(RecordedLogsAction.ShareAllClick) }
                        },
                    )
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        when (val viewState = state.viewState) {
            is RecordedLogsState.ViewState.Content -> {
                RecordedLogsContent(
                    viewState = viewState,
                    onShareItemClick = remember(viewModel) {
                        { viewModel.trySendAction(RecordedLogsAction.ShareClick(it)) }
                    },
                    onDeleteItemClick = remember(viewModel) {
                        { viewModel.trySendAction(RecordedLogsAction.DeleteClick(it)) }
                    },
                )
            }

            RecordedLogsState.ViewState.Empty -> {
                BitwardenEmptyContent(
                    text = stringResource(id = R.string.no_logs_recorded),
                    illustrationData = IconData.Local(iconRes = R.drawable.il_secure_devices),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            RecordedLogsState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun RecordedLogsOverflowMenu(
    isOverflowEnabled: Boolean,
    onDeleteAllClick: () -> Unit,
    onShareAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenOverflowActionItem(
        modifier = modifier,
        menuItemDataList = persistentListOf(
            OverflowMenuItemData(
                text = stringResource(id = R.string.share_all),
                onClick = onShareAllClick,
                isEnabled = isOverflowEnabled,
            ),
            OverflowMenuItemData(
                text = stringResource(id = R.string.delete_all),
                onClick = onDeleteAllClick,
                isEnabled = isOverflowEnabled,
                color = BitwardenTheme.colorScheme.status.error,
            ),
        ),
    )
}

@Composable
private fun RecordedLogsContent(
    viewState: RecordedLogsState.ViewState.Content,
    onDeleteItemClick: (RecordedLogsState.DisplayItem) -> Unit,
    onShareItemClick: (RecordedLogsState.DisplayItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }

        itemsIndexed(
            items = viewState.items,
            key = { _, item -> item.toString() },
        ) { index, item ->
            LogRow(
                displayableItem = item,
                cardStyle = viewState.items.toListItemCardStyle(index = index),
                onDeleteItemClick = onDeleteItemClick,
                onShareItemClick = onShareItemClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LogRow(
    displayableItem: RecordedLogsState.DisplayItem,
    onDeleteItemClick: (RecordedLogsState.DisplayItem) -> Unit,
    onShareItemClick: (RecordedLogsState.DisplayItem) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                paddingTop = 12.dp,
                paddingBottom = 12.dp,
                paddingStart = 16.dp,
                paddingEnd = 4.dp,
            ),
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = displayableItem.title(),
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = 2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayableItem.subtextStart(),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f),
                )
                displayableItem.subtextEnd?.let {
                    Spacer(modifier = Modifier.width(width = 4.dp))
                    Text(
                        text = it(),
                        style = BitwardenTheme.typography.bodyMedium,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(weight = 1f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(width = 12.dp))
        BitwardenOverflowActionItem(
            menuItemDataList = persistentListOf(
                OverflowMenuItemData(
                    text = stringResource(id = R.string.share),
                    onClick = { onShareItemClick(displayableItem) },
                ),
                OverflowMenuItemData(
                    text = stringResource(id = R.string.delete),
                    onClick = { onDeleteItemClick(displayableItem) },
                    color = BitwardenTheme.colorScheme.status.error,
                    isEnabled = displayableItem.isDeletedEnabled,
                ),
            ),
            vectorIconRes = R.drawable.ic_ellipsis_horizontal,
            testTag = "Options",
        )
    }
}
