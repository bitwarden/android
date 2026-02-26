package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the screen for adding or editing a folder item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun FolderAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: FolderAddEditViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    var shouldShowConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is FolderAddEditEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    FolderAddEditItemDialogs(
        dialogState = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(FolderAddEditAction.DismissDialog) }
        },
    )

    if (shouldShowConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = null,
            message = stringResource(id = BitwardenString.do_you_really_want_to_delete),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            confirmButtonText = stringResource(id = BitwardenString.delete),
            onDismissClick = { shouldShowConfirmationDialog = false },
            onConfirmClick = {
                shouldShowConfirmationDialog = false
                viewModel.trySendAction(FolderAddEditAction.DeleteClick)
            },
            onDismissRequest = { shouldShowConfirmationDialog = false },
            confirmTextColor = BitwardenTheme.colorScheme.status.error,
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName.invoke(),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(FolderAddEditAction.CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(FolderAddEditAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                    if (state.shouldShowOverflowMenu) {
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(BitwardenString.more),
                            menuItemDataList = persistentListOf(
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.delete),
                                    onClick = { shouldShowConfirmationDialog = true },
                                ),
                            ),
                        )
                    }
                },
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is FolderAddEditState.ViewState.Content -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Spacer(modifier = Modifier.height(height = 12.dp))
                    BitwardenTextField(
                        label = stringResource(id = BitwardenString.name),
                        value = viewState.folderName,
                        onValueChange = remember(viewModel) {
                            { viewModel.trySendAction(FolderAddEditAction.NameTextChange(it)) }
                        },
                        textFieldTestTag = "FolderNameField",
                        cardStyle = CardStyle.Full,
                        modifier = Modifier
                            .standardHorizontalMargin()
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }

            is FolderAddEditState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is FolderAddEditState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FolderAddEditItemDialogs(
    dialogState: FolderAddEditState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is FolderAddEditState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.label())
        }

        is FolderAddEditState.DialogState.Error -> BitwardenBasicDialog(
            title = stringResource(id = BitwardenString.an_error_has_occurred),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
            throwable = dialogState.throwable,
        )

        null -> Unit
    }
}
