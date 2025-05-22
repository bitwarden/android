package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.bottomDivider
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold

/**
 * Displays the block auto-fill screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockAutoFillScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockAutoFillViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            BlockAutoFillEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    BlockAutoFillDialogs(
        dialogState = state.dialog,
        onUriTextChange = remember(viewModel) {
            { viewModel.trySendAction(BlockAutoFillAction.UriTextChange(uri = it)) }
        },
        onSaveClick = remember(viewModel) {
            { viewModel.trySendAction(BlockAutoFillAction.SaveUri(newUri = it)) }
        },
        onRemoveClick = remember(viewModel) {
            { viewModel.trySendAction(BlockAutoFillAction.RemoveUriClick(it)) }
        },
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(BlockAutoFillAction.DismissDialog) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.block_auto_fill),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(BlockAutoFillAction.BackClick) }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                BitwardenFloatingActionButton(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(BlockAutoFillAction.AddUriClick) }
                    },
                    painter = rememberVectorPainter(id = R.drawable.ic_plus_large),
                    contentDescription = stringResource(id = R.string.add_item),
                    modifier = Modifier.testTag(tag = "AddItemButton"),
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val viewState = state.viewState) {
                is BlockAutoFillState.ViewState.Content -> {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.auto_fill_will_not_be_offered_for_these_ur_is,
                                ),
                                color = BitwardenTheme.colorScheme.text.primary,
                                style = BitwardenTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }

                    items(viewState.blockedUris, key = { it }) { uri ->
                        BlockAutoFillListItem(
                            label = uri,
                            onClick = remember(viewModel) {
                                {
                                    viewModel.trySendAction(
                                        BlockAutoFillAction.EditUriClick(uri),
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                        )
                    }
                }

                is BlockAutoFillState.ViewState.Empty -> {
                    item {
                        BlockAutoFillNoItems(
                            addItemClickAction = remember(viewModel) {
                                { viewModel.trySendAction(BlockAutoFillAction.AddUriClick) }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun BlockAutoFillDialogs(
    dialogState: BlockAutoFillState.DialogState? = null,
    onUriTextChange: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is BlockAutoFillState.DialogState.AddEdit -> {
            AddEditBlockedUriDialog(
                uri = dialogState.uri,
                isEdit = dialogState.originalUri != null,
                errorMessage = dialogState.errorMessage?.invoke(),
                onUriChange = onUriTextChange,
                onDismissRequest = onDismissRequest,
                onDeleteClick = if (dialogState.isEdit) {
                    { dialogState.originalUri?.let { onRemoveClick(it) } }
                } else {
                    null
                },
                onCancelClick = onDismissRequest,
                onSaveClick = onSaveClick,
            )
        }

        null -> Unit
    }
}

/**
 * No items view for the [BlockAutoFillScreen].
 */
@Composable
private fun BlockAutoFillNoItems(
    addItemClickAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(height = 24.dp))
        Image(
            painter = rememberVectorPainter(
                id = R.drawable.blocked_uri,
            ),
            contentDescription = null,
            modifier = Modifier
                .size(size = 124.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(id = R.string.auto_fill_will_not_be_offered_for_these_ur_is),
            style = BitwardenTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = stringResource(id = R.string.new_blocked_uri),
            onClick = addItemClickAction,
        )
    }
}

@Composable
private fun BlockAutoFillListItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = onClick,
            )
            .bottomDivider(paddingStart = 16.dp)
            .padding(end = 8.dp, top = 16.dp, bottom = 16.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            text = label,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
        )
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_pencil_square),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}
