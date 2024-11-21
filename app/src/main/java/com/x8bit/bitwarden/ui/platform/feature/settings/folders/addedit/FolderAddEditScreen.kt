package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
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
    val context = LocalContext.current

    var shouldShowConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is FolderAddEditEvent.NavigateBack -> onNavigateBack.invoke()
            is FolderAddEditEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
            }
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
            message = stringResource(id = R.string.do_you_really_want_to_delete),
            dismissButtonText = stringResource(id = R.string.cancel),
            confirmButtonText = stringResource(id = R.string.delete),
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
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(FolderAddEditAction.CloseClick) }
                },
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(FolderAddEditAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                    if (state.shouldShowOverflowMenu) {
                        BitwardenOverflowActionItem(
                            menuItemDataList = persistentListOf(
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.delete),
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
                    BitwardenTextField(
                        label = stringResource(id = R.string.name),
                        value = viewState.folderName,
                        onValueChange = remember(viewModel) {
                            { viewModel.trySendAction(FolderAddEditAction.NameTextChange(it)) }
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    )
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
            title = stringResource(id = R.string.an_error_has_occurred),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
        )

        null -> Unit
    }
}
