package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.handlers.AddSendHandlers

/**
 * Displays new send UX.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSendScreen(
    viewModel: AddSendViewModel = hiltViewModel(),
    intentHandler: IntentHandler = IntentHandler(LocalContext.current),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is AddSendEvent.NavigateBack -> onNavigateBack()
            is AddSendEvent.ShowShareSheet -> {
                intentHandler.shareText(event.message)
            }

            is AddSendEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    AddSendDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(AddSendAction.DismissDialogClick) }
        },
    )

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.CloseClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.SaveClick) }
                        },
                    )
                    if (!state.isAddMode) {
                        BitwardenOverflowActionItem(
                            menuItemDataList = persistentListOfNotNull(
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.remove_password),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                AddSendAction.RemovePasswordClick,
                                            )
                                        }
                                    },
                                )
                                    .takeIf { state.hasPassword },
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.copy_link),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddSendAction.CopyLinkClick) }
                                    },
                                ),
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.share_link),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddSendAction.ShareLinkClick) }
                                    },
                                ),
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.delete),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddSendAction.DeleteClick) }
                                    },
                                ),
                            ),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .padding(paddingValues = innerPadding)

        when (val viewState = state.viewState) {
            is AddSendState.ViewState.Content -> AddSendContent(
                state = viewState,
                isAddMode = state.isAddMode,
                addSendHandlers = remember(viewModel) { AddSendHandlers.create(viewModel) },
                modifier = modifier,
            )

            is AddSendState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                modifier = modifier,
            )

            AddSendState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AddSendDialogs(
    dialogState: AddSendState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is AddSendState.DialogState.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is AddSendState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(dialogState.message),
        )

        null -> Unit
    }
}
