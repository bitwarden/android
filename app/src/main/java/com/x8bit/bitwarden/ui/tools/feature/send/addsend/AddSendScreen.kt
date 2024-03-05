package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.LocalExitManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalPermissionsManager
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
    exitManager: ExitManager = LocalExitManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val addSendHandlers = remember(viewModel) { AddSendHandlers.create(viewModel) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val resources = context.resources

    val fileChooserLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            addSendHandlers.onFileChoose(it)
        }
    }

    BackHandler(
        onBack = remember(viewModel) {
            { viewModel.trySendAction(AddSendAction.CloseClick) }
        },
    )
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AddSendEvent.ExitApp -> exitManager.exitApplication()

            is AddSendEvent.NavigateBack -> onNavigateBack()

            is AddSendEvent.ShowChooserSheet -> {
                fileChooserLauncher.launch(
                    intentManager.createFileChooserIntent(event.withCameraOption),
                )
            }

            is AddSendEvent.ShowShareSheet -> {
                intentManager.shareText(event.message)
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
    var shouldShowDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowDeleteConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete),
            message = stringResource(id = R.string.are_you_sure_delete_send),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = remember(viewModel) {
                {
                    viewModel.trySendAction(AddSendAction.DeleteClick)
                    shouldShowDeleteConfirmationDialog = false
                }
            },
            onDismissClick = { shouldShowDeleteConfirmationDialog = false },
            onDismissRequest = { shouldShowDeleteConfirmationDialog = false },
        )
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = R.drawable.ic_close),
                    navigationIconContentDescription = stringResource(id = R.string.close),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(AddSendAction.CloseClick) }
                    },
                )
                    .takeUnless { state.isShared },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        isEnabled = !state.policyDisablesSend,
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
                                    .takeIf { state.hasPassword && !state.policyDisablesSend },
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.copy_link),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddSendAction.CopyLinkClick) }
                                    },
                                )
                                    .takeIf { !state.policyDisablesSend },
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.share_link),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddSendAction.ShareLinkClick) }
                                    },
                                )
                                    .takeIf { !state.policyDisablesSend },
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.delete),
                                    onClick = { shouldShowDeleteConfirmationDialog = true },
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
                policyDisablesSend = state.policyDisablesSend,
                policySendOptionsInEffect = state.shouldDisplayPolicyWarning,
                isAddMode = state.isAddMode,
                isShared = state.isShared,
                addSendHandlers = addSendHandlers,
                permissionsManager = permissionsManager,
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
