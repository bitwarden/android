package com.x8bit.bitwarden.ui.tools.feature.send

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.tools.feature.send.handlers.SendHandlers
import kotlinx.collections.immutable.persistentListOf

/**
 * UI for the send screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onNavigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
    intentHandler: IntentHandler = IntentHandler(context = LocalContext.current),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is SendEvent.NavigateNewSend -> onNavigateToAddSend()

            is SendEvent.NavigateToEditSend -> onNavigateToEditSend(event.sendId)

            is SendEvent.NavigateToAboutSend -> {
                intentHandler.launchUri("https://bitwarden.com/products/send".toUri())
            }

            is SendEvent.ShowShareSheet -> {
                intentHandler.shareText(event.url)
            }

            is SendEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    SendDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(SendAction.DismissDialog) }
        },
    )

    val sendHandlers = remember(viewModel) { SendHandlers.create(viewModel) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.send),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_sends),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SendAction.SearchClick) }
                        },
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.sync),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.SyncClick) }
                                },
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.lock),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.LockClick) }
                                },
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.about_send),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.AboutSendClick) }
                                },
                            ),
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.viewState.shouldDisplayFab,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(SendAction.AddSendClick) }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .padding(padding)
        when (val viewState = state.viewState) {
            is SendState.ViewState.Content -> SendContent(
                modifier = modifier,
                state = viewState,
                sendHandlers = sendHandlers,
            )

            SendState.ViewState.Empty -> SendEmpty(
                modifier = modifier,
                onAddItemClick = remember(viewModel) {
                    { viewModel.trySendAction(SendAction.AddSendClick) }
                },
            )

            is SendState.ViewState.Error -> BitwardenErrorContent(
                modifier = modifier,
                message = viewState.message(),
                onTryAgainClick = remember(viewModel) {
                    { viewModel.trySendAction(SendAction.RefreshClick) }
                },
            )

            SendState.ViewState.Loading -> BitwardenLoadingContent(modifier = modifier)
        }
    }
}

@Composable
private fun SendDialogs(
    dialogState: SendState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is SendState.DialogState.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is SendState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(dialogState.message),
        )

        null -> Unit
    }
}
