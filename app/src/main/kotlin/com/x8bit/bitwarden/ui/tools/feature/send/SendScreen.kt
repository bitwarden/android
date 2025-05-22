package com.x8bit.bitwarden.ui.tools.feature.send

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.RegisterScreenDataOnLifecycleEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.model.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalAppResumeStateManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.handlers.SendHandlers
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.selectionText
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import kotlinx.collections.immutable.persistentListOf

/**
 * UI for the send screen.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onNavigateToAddEditSend: (AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (ViewSendRoute) -> Unit,
    onNavigateToSendFilesList: () -> Unit,
    onNavigateToSendTextList: () -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    appResumeStateManager: AppResumeStateManager = LocalAppResumeStateManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = remember(viewModel) {
            { viewModel.trySendAction(SendAction.RefreshPull) }
        },
    )

    RegisterScreenDataOnLifecycleEffect(
        appResumeStateManager = appResumeStateManager,
    ) {
        AppResumeScreenData.SendScreen
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is SendEvent.NavigateToSearch -> onNavigateToSearchSend(SearchType.Sends.All)

            is SendEvent.NavigateNewSend -> {
                onNavigateToAddEditSend(
                    AddEditSendRoute(modeType = ModeType.ADD, sendType = event.sendType),
                )
            }

            is SendEvent.NavigateToEditSend -> {
                onNavigateToAddEditSend(
                    AddEditSendRoute(
                        modeType = ModeType.EDIT,
                        sendType = event.sendType,
                        sendId = event.sendId,
                    ),
                )
            }

            is SendEvent.NavigateToViewSend -> {
                onNavigateToViewSend(
                    ViewSendRoute(sendId = event.sendId, sendType = event.sendType),
                )
            }

            is SendEvent.NavigateToAboutSend -> {
                intentManager.launchUri("https://bitwarden.com/products/send".toUri())
            }

            is SendEvent.ShowShareSheet -> {
                intentManager.shareText(event.url)
            }

            is SendEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }

            SendEvent.NavigateToFileSends -> onNavigateToSendFilesList()
            SendEvent.NavigateToTextSends -> onNavigateToSendTextList()
        }
    }

    SendDialogs(
        dialogState = state.dialogState,
        onAddSendSelected = remember(viewModel) {
            { viewModel.trySendAction(SendAction.AddSendSelected(it)) }
        },
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(SendAction.DismissDialog) }
        },
    )

    val sendHandlers = remember(viewModel) { SendHandlers.create(viewModel) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
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
                BitwardenFloatingActionButton(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(SendAction.AddSendClick) }
                    },
                    painter = rememberVectorPainter(id = R.drawable.ic_plus_large),
                    contentDescription = stringResource(id = R.string.add_item),
                    modifier = Modifier.testTag(tag = "AddItemButton"),
                )
            }
        },
        pullToRefreshState = pullToRefreshState,
    ) {
        val modifier = Modifier
            .fillMaxSize()
        when (val viewState = state.viewState) {
            is SendState.ViewState.Content -> SendContent(
                policyDisablesSend = state.policyDisablesSend,
                state = viewState,
                sendHandlers = sendHandlers,
                modifier = modifier,
            )

            SendState.ViewState.Empty -> SendEmpty(
                policyDisablesSend = state.policyDisablesSend,
                onAddItemClick = remember(viewModel) {
                    { viewModel.trySendAction(SendAction.AddSendClick) }
                },
                modifier = modifier,
            )

            is SendState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                onTryAgainClick = remember(viewModel) {
                    { viewModel.trySendAction(SendAction.RefreshClick) }
                },
                modifier = modifier,
            )

            SendState.ViewState.Loading -> BitwardenLoadingContent(modifier = modifier)
        }
    }
}

@Composable
private fun SendDialogs(
    dialogState: SendState.DialogState?,
    onAddSendSelected: (SendItemType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is SendState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
            throwable = dialogState.throwable,
        )

        is SendState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        SendState.DialogState.SelectSendAddType -> BitwardenSelectionDialog(
            title = stringResource(id = R.string.type),
            onDismissRequest = onDismissRequest,
        ) {
            SendItemType.entries.forEach {
                BitwardenBasicDialogRow(
                    text = it.selectionText(),
                    onClick = { onAddSendSelected(it) },
                )
            }
        }

        null -> Unit
    }
}
