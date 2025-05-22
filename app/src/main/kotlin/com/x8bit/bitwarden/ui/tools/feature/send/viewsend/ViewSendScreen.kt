package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import android.widget.Toast
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedErrorButton
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
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

            is ViewSendEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
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
                    navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                    navigationIconContentDescription = stringResource(id = R.string.close),
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
                    painter = rememberVectorPainter(id = R.drawable.ic_pencil),
                    contentDescription = stringResource(id = R.string.edit_send),
                    modifier = Modifier.testTag(tag = "EditItemButton"),
                )
            }
        },
    ) {
        ViewSendScreenContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onCopyClick = remember(viewModel) {
                { viewModel.trySendAction(ViewSendAction.CopyClick) }
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
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val viewState = state.viewState) {
        is ViewSendState.ViewState.Content -> {
            ViewStateContent(
                state = viewState,
                onCopyClick = onCopyClick,
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
            label = stringResource(id = R.string.copy),
            onClick = onCopyClick,
            icon = rememberVectorPainter(id = R.drawable.ic_copy_small),
            cardStyle = CardStyle.Middle(hasDivider = false),
            cardInsets = PaddingValues(top = 16.dp, bottom = 6.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenOutlinedButton(
            label = stringResource(id = R.string.share),
            onClick = onShareClick,
            icon = rememberVectorPainter(id = R.drawable.ic_share_small),
            cardStyle = CardStyle.Bottom,
            cardInsets = PaddingValues(top = 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.send_details),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
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
            label = stringResource(id = R.string.send_name_required),
            value = state.sendName,
            onValueChange = {},
            readOnly = true,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))

        BitwardenTextField(
            label = stringResource(id = R.string.deletion_date),
            value = state.deletionDate,
            onValueChange = {},
            readOnly = true,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        AdditionalOptions(state = state)

        DeleteButton(
            onDeleteClick = onDeleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
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
            title = stringResource(id = R.string.delete),
            message = stringResource(id = R.string.are_you_sure_delete_send),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                onDeleteClick()
                shouldShowDeleteConfirmationDialog = false
            },
            onDismissClick = { shouldShowDeleteConfirmationDialog = false },
            onDismissRequest = { shouldShowDeleteConfirmationDialog = false },
        )
    }
    BitwardenOutlinedErrorButton(
        label = stringResource(id = R.string.delete_send),
        onClick = { shouldShowDeleteConfirmationDialog = true },
        icon = rememberVectorPainter(id = R.drawable.ic_trash_small),
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
            text = stringResource(id = R.string.share_link),
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.weight(weight = 1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fileType.fileSize,
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TextSendContent(
    textType: ViewSendState.ViewState.Content.SendType.TextType,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.text_to_share),
        value = textType.textToShare,
        onValueChange = {},
        readOnly = true,
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.AdditionalOptions(
    state: ViewSendState.ViewState.Content,
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
                    label = stringResource(id = R.string.maximum_access_count),
                    value = it,
                    supportingText = R.string.current_access_count
                        .asText(state.currentAccessCount)
                        .invoke(),
                    onValueChange = {},
                    isDecrementEnabled = false,
                    isIncrementEnabled = false,
                    range = 0..Int.MAX_VALUE,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .testTag(tag = "SendMaxAccessCount")
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
                    label = stringResource(id = R.string.private_notes),
                    readOnly = true,
                    value = it,
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
