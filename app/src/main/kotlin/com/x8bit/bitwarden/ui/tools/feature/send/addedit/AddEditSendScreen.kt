package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.appbar.model.TopAppBarDividerStyle
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.handlers.AddEditSendHandlers

/**
 * Displays new send UX.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSendScreen(
    viewModel: AddEditSendViewModel = hiltViewModel(),
    exitManager: ExitManager = LocalExitManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateBack: () -> Unit,
    onNavigateUpToSearchOrRoot: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val addSendHandlers = remember(viewModel) { AddEditSendHandlers.create(viewModel) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val fileChooserLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            addSendHandlers.onFileChoose(it)
        }
    }

    val snackbarHostState = rememberBitwardenSnackbarHostState()
    BackHandler(
        onBack = remember(viewModel) {
            { viewModel.trySendAction(AddEditSendAction.CloseClick) }
        },
    )
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AddEditSendEvent.ExitApp -> exitManager.exitApplication()

            is AddEditSendEvent.NavigateBack -> onNavigateBack()

            is AddEditSendEvent.NavigateUpToSearchOrRoot -> onNavigateUpToSearchOrRoot()

            is AddEditSendEvent.ShowChooserSheet -> {
                fileChooserLauncher.launch(
                    intentManager.createFileChooserIntent(event.withCameraOption),
                )
            }

            is AddEditSendEvent.ShowShareSheet -> {
                intentManager.shareText(event.message)
            }

            is AddEditSendEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    AddEditSendDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(AddEditSendAction.DismissDialogClick) }
        },
    )
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                    navigationIconContentDescription = stringResource(id = BitwardenString.close),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(AddEditSendAction.CloseClick) }
                    },
                )
                    .takeUnless { state.isShared },
                dividerStyle = TopAppBarDividerStyle.NONE,
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.save),
                        isEnabled = !state.policyDisablesSend,
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddEditSendAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                    if (!state.isAddMode) {
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(BitwardenString.more),
                            menuItemDataList = persistentListOfNotNull(
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.remove_password),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                AddEditSendAction.RemovePasswordClick,
                                            )
                                        }
                                    },
                                )
                                    .takeIf { state.hasPassword && !state.policyDisablesSend },
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.copy_link),
                                    onClick = remember(viewModel) {
                                        { viewModel.trySendAction(AddEditSendAction.CopyLinkClick) }
                                    },
                                )
                                    .takeIf { !state.policyDisablesSend },
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.share_link),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                AddEditSendAction.ShareLinkClick,
                                            )
                                        }
                                    },
                                )
                                    .takeIf { !state.policyDisablesSend },
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        val modifier = Modifier
            .fillMaxSize()

        when (val viewState = state.viewState) {
            is AddEditSendState.ViewState.Content -> AddEditSendContent(
                state = viewState,
                policyDisablesSend = state.policyDisablesSend,
                policySendOptionsInEffect = state.shouldDisplayPolicyWarning,
                isAddMode = state.isAddMode,
                isShared = state.isShared,
                addSendHandlers = addSendHandlers,
                permissionsManager = permissionsManager,
                modifier = modifier,
            )

            is AddEditSendState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                modifier = modifier,
            )

            AddEditSendState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AddEditSendDialogs(
    dialogState: AddEditSendState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is AddEditSendState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
            throwable = dialogState.throwable,
        )

        is AddEditSendState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}
