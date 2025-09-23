package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedErrorButton
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType

/**
 * Displays view send screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSendScreen(
    viewModel: ViewSendViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ViewSendEvent.NavigateBack -> onNavigateBack()
            is ViewSendEvent.NavigateToEdit -> {
                onNavigateToAddEditSend(
                    AddEditSendRoute(
                        sendType = event.sendType,
                        modeType = ModeType.EDIT,
                        sendId = event.sendId,
                    ),
                )
            }

            is ViewSendEvent.ShareText -> {
                intentManager.shareText(text = event.text(resources).toString())
            }

            is ViewSendEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    ViewSendDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(ViewSendAction.DialogDismiss) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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
                        { viewModel.trySendAction(ViewSendAction.CloseClick) }
                    },
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isFabVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                BitwardenFloatingActionButton(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(ViewSendAction.EditClick) }
                    },
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_pencil),
                    contentDescription = stringResource(id = BitwardenString.edit_send),
                    modifier = Modifier.testTag(tag = "EditItemButton"),
                )
            }
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        ViewSendScreenContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onCopyClick = remember(viewModel) {
                { viewModel.trySendAction(ViewSendAction.CopyClick) }
            },
            onCopyNotesClick = remember(viewModel) {
                { viewModel.trySendAction(ViewSendAction.CopyNotesClick) }
            },
            onDeleteClick = remember(viewModel) {
                { viewModel.trySendAction(ViewSendAction.DeleteClick) }
            },
            onShareClick = remember(viewModel) {
                { viewModel.trySendAction(ViewSendAction.ShareClick) }
            },
        )
    }
}

@Composable
private fun ViewSendDialogs(
    dialogState: ViewSendState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is ViewSendState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                throwable = dialogState.throwable,
                onDismissRequest = onDismissRequest,
            )
        }

        is ViewSendState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        null -> Unit
    }
}

@Composable
private fun ViewSendScreenContent(
    state: ViewSendState,
    onCopyClick: () -> Unit,
    onCopyNotesClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val viewState = state.viewState) {
        is ViewSendState.ViewState.Content -> {
            ViewStateContent(
                state = viewState,
                onCopyClick = onCopyClick,
                onCopyNotesClick = onCopyNotesClick,
                onDeleteClick = onDeleteClick,
                onShareClick = onShareClick,
                modifier = modifier,
            )
        }

        is ViewSendState.ViewState.Error -> {
            BitwardenErrorContent(
                message = viewState.message(),
                modifier = modifier,
            )
        }

        ViewSendState.ViewState.Loading -> {
            BitwardenLoadingContent(modifier = modifier)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ViewStateContent(
    state: ViewSendState.ViewState.Content,
    onCopyClick: () -> Unit,
    onCopyNotesClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(state = rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        ShareLinkSection(
            shareLink = state.shareLink,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.copy),
            onClick = onCopyClick,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_copy_small),
            cardStyle = CardStyle.Middle(hasDivider = false),
            cardInsets = PaddingValues(top = 16.dp, bottom = 6.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag(tag = "ViewSendCopyButton"),
        )
        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.share),
            onClick = onShareClick,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_share_small),
            cardStyle = CardStyle.Bottom,
            cardInsets = PaddingValues(top = 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag(tag = "ViewSendShareButton"),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = BitwardenString.send_details),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.send_name_required),
            value = state.sendName,
            onValueChange = {},
            readOnly = true,
            cardStyle = CardStyle.Full,
            textFieldTestTag = "ViewSendNameField",
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        when (val sendType = state.sendType) {
            is ViewSendState.ViewState.Content.SendType.FileType -> {
                FileSendContent(
                    fileType = sendType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }

            is ViewSendState.ViewState.Content.SendType.TextType -> {
                TextSendContent(
                    textType = sendType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }
        Spacer(modifier = Modifier.height(height = 8.dp))

        BitwardenTextField(
            label = stringResource(id = BitwardenString.deletion_date),
            value = state.deletionDate,
            onValueChange = {},
            readOnly = true,
            cardStyle = CardStyle.Full,
            textFieldTestTag = "ViewSendDeletionDateField",
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        AdditionalOptions(
            state = state,
            onCopyNotesClick = onCopyNotesClick,
        )

        DeleteButton(
            onDeleteClick = onDeleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("ViewSendDeleteButton"),
        )
        Spacer(modifier = Modifier.height(height = 88.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DeleteButton(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDeleteConfirmationDialog by rememberSaveable { mutableStateOf(value = false) }
    if (shouldShowDeleteConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.delete),
            message = stringResource(id = BitwardenString.are_you_sure_delete_send),
            confirmButtonText = stringResource(id = BitwardenString.yes),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                onDeleteClick()
                shouldShowDeleteConfirmationDialog = false
            },
            onDismissClick = { shouldShowDeleteConfirmationDialog = false },
            onDismissRequest = { shouldShowDeleteConfirmationDialog = false },
        )
    }
    BitwardenOutlinedErrorButton(
        label = stringResource(id = BitwardenString.delete_send),
        onClick = { shouldShowDeleteConfirmationDialog = true },
        icon = rememberVectorPainter(id = BitwardenDrawable.ic_trash_small),
        modifier = modifier,
    )
}

/**
 * A default content block which displays a header with an optional subtitle and an icon.
 * Implemented to match design component.
 */
@Composable
private fun ShareLinkSection(
    shareLink: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .cardStyle(
                cardStyle = CardStyle.Top(dividerPadding = 0.dp),
                paddingHorizontal = 16.dp,
                paddingVertical = 12.dp,
            ),
    ) {
        Text(
            text = stringResource(id = BitwardenString.send_link),
            style = BitwardenTheme.typography.titleSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 4.dp))
        Text(
            text = shareLink,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag = "ViewSendShareLinkText"),
        )
    }
}

@Composable
private fun FileSendContent(
    fileType: ViewSendState.ViewState.Content.SendType.FileType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = CardStyle.Full,
                paddingHorizontal = 16.dp,
                paddingVertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = fileType.fileName,
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(weight = 1f)
                .testTag("ViewSendFileNameText"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fileType.fileSize,
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodyLarge,
            modifier = Modifier.testTag("ViewSendFileSizeText"),
        )
    }
}

@Composable
private fun TextSendContent(
    textType: ViewSendState.ViewState.Content.SendType.TextType,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = BitwardenString.text_to_share),
        value = textType.textToShare,
        onValueChange = {},
        readOnly = true,
        cardStyle = CardStyle.Full,
        textFieldTestTag = "ViewSendContentText",
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.AdditionalOptions(
    state: ViewSendState.ViewState.Content,
    onCopyNotesClick: () -> Unit,
) {
    if (state.maxAccessCount == null && state.notes == null) {
        Spacer(modifier = Modifier.height(height = 16.dp))
        return
    }
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    BitwardenExpandingHeader(
        isExpanded = isExpanded,
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier
            .testTag(tag = "ViewSendAdditionalOptions")
            .standardHorizontalMargin()
            .fillMaxWidth(),
    )
    // Hide all content if not expanded:
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = Modifier.clipToBounds(),
    ) {
        Column {
            state.maxAccessCount?.let {
                BitwardenStepper(
                    label = stringResource(id = BitwardenString.maximum_access_count),
                    value = it,
                    supportingText = BitwardenString.current_access_count
                        .asText(state.currentAccessCount)
                        .invoke(),
                    onValueChange = {},
                    isDecrementEnabled = false,
                    isIncrementEnabled = false,
                    range = 0..Int.MAX_VALUE,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .testTag(tag = "ViewSendMaxAccessCount")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
            state.notes?.let {
                if (state.maxAccessCount != null) {
                    // If the stepper is present, we need a spacer between these 2 items
                    Spacer(modifier = Modifier.height(height = 8.dp))
                }
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.private_notes),
                    readOnly = true,
                    value = it,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(id = BitwardenString.copy_notes),
                            onClick = onCopyNotesClick,
                            modifier = Modifier.testTag(tag = "ViewSendNotesCopyButton"),
                        )
                    },
                    singleLine = false,
                    onValueChange = {},
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .testTag(tag = "ViewSendNotes")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
            Spacer(modifier = Modifier.height(height = 16.dp))
        }
    }
}
