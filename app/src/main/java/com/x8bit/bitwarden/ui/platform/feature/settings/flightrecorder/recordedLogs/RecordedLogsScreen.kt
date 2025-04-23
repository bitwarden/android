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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.x8bit.bitwarden.ui.platform.components.snackbar.rememberBitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the flight recorder recorded logs screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun RecordedLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordedLogsViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel) { event ->
        when (event) {
            RecordedLogsEvent.NavigateBack -> onNavigateBack()
            is RecordedLogsEvent.ShareLog -> {
                intentManager.shareFile(fileUri = event.uri.toUri())
            }

            is RecordedLogsEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    snackbarData = BitwardenSnackbarData(message = event.text),
                )
            }
        }
    }

    RecordedLogsDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(RecordedLogsAction.DismissDialog) }
        },
    )

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
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
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
    var showDeletionDialog by rememberSaveable { mutableStateOf(value = false) }
    if (showDeletionDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete_logs),
            message = stringResource(id = R.string.do_you_really_want_to_delete_all_recorded_logs),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onDismissRequest = { showDeletionDialog = false },
            onDismissClick = { showDeletionDialog = false },
            onConfirmClick = {
                onDeleteAllClick()
                showDeletionDialog = false
            },
        )
    }
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
                onClick = { showDeletionDialog = true },
                isEnabled = isOverflowEnabled,
                color = BitwardenTheme.colorScheme.status.error,
            ),
        ),
    )
}

@Composable
private fun RecordedLogsDialogs(
    dialogState: RecordedLogsState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is RecordedLogsState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                throwable = dialogState.error,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
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
    var showDeletionDialog by rememberSaveable { mutableStateOf(value = false) }
    if (showDeletionDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete_log),
            message = stringResource(id = R.string.do_you_really_want_to_delete_this_log),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onDismissRequest = { showDeletionDialog = false },
            onDismissClick = { showDeletionDialog = false },
            onConfirmClick = {
                onDeleteItemClick(displayableItem)
                showDeletionDialog = false
            },
        )
    }
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
                    onClick = { showDeletionDialog = true },
                    color = BitwardenTheme.colorScheme.status.error,
                    isEnabled = displayableItem.isDeletedEnabled,
                ),
            ),
            vectorIconRes = R.drawable.ic_ellipsis_horizontal,
            testTag = "Options",
        )
    }
}
