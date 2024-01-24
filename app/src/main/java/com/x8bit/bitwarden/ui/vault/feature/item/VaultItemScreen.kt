package com.x8bit.bitwarden.ui.vault.feature.item

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCardItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultLoginItemTypeHandlers

/**
 * Displays the vault item screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemScreen(
    viewModel: VaultItemViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
    onNavigateToVaultAddEditItem: (vaultItemId: String, isClone: Boolean) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    val confirmDeleteClickAction = remember(viewModel) {
        { viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick) }
    }
    val confirmRestoreAction = remember(viewModel) {
        { viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick) }
    }
    var pendingDeleteCipher by rememberSaveable { mutableStateOf(false) }
    var pendingRestoreCipher by rememberSaveable { mutableStateOf(false) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultItemEvent.NavigateBack -> onNavigateBack()

            is VaultItemEvent.NavigateToAddEdit -> {
                onNavigateToVaultAddEditItem(event.itemId, event.isClone)
            }

            is VaultItemEvent.NavigateToPasswordHistory -> {
                // TODO Implement password history in BIT-617
                Toast.makeText(context, "Not yet implemented.", Toast.LENGTH_SHORT).show()
            }

            is VaultItemEvent.NavigateToUri -> intentManager.launchUri(event.uri.toUri())

            is VaultItemEvent.NavigateToAttachments -> onNavigateToAttachments(event.itemId)

            is VaultItemEvent.NavigateToMoveToOrganization -> {
                onNavigateToMoveToOrganization(event.itemId)
            }

            is VaultItemEvent.NavigateToCollections -> {
                // TODO implement Collections in BIT-1575
                Toast.makeText(context, "Not yet implemented.", Toast.LENGTH_SHORT).show()
            }

            is VaultItemEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    VaultItemDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick) }
        },
        onSubmitMasterPassword = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.Common.MasterPasswordSubmit(it)) }
        },
    )

    if (pendingDeleteCipher) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete),
            message = stringResource(id = R.string.do_you_really_want_to_soft_delete_cipher),
            confirmButtonText = stringResource(id = R.string.ok),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                pendingDeleteCipher = false
                confirmDeleteClickAction()
            },
            onDismissClick = {
                pendingDeleteCipher = false
            },
            onDismissRequest = {
                pendingDeleteCipher = false
            },
        )
    }
    if (pendingRestoreCipher) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.restore),
            message = stringResource(id = R.string.do_you_really_want_to_restore_cipher),
            confirmButtonText = stringResource(id = R.string.ok),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                pendingRestoreCipher = false
                confirmRestoreAction()
            },
            onDismissClick = {
                pendingRestoreCipher = false
            },
            onDismissRequest = {
                pendingRestoreCipher = false
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.view_item),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultItemAction.Common.CloseClick) }
                },
                actions = {
                    if (state.isCipherDeleted) {
                        BitwardenTextButton(
                            label = stringResource(id = R.string.restore),
                            onClick = { pendingRestoreCipher = true },
                        )
                    }
                    // TODO make action list dependent on item being in an organization BIT-1446
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOfNotNull(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.attachments),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultItemAction.Common.AttachmentsClick,
                                        )
                                    }
                                },
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.clone),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(VaultItemAction.Common.CloneClick) }
                                },
                            )
                                .takeUnless { state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.move_to_organization),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultItemAction.Common.MoveToOrganizationClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.collections),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultItemAction.Common.CollectionsClick,
                                        )
                                    }
                                },
                            )
                                .takeIf { state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.delete),
                                onClick = { pendingDeleteCipher = true },
                            ),
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isFabVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultItemAction.Common.EditClick) }
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = stringResource(id = R.string.edit_item),
                    )
                }
            }
        },
    ) { innerPadding ->
        VaultItemContent(
            viewState = state.viewState,
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .padding(innerPadding),
            vaultCommonItemTypeHandlers = remember(viewModel) {
                VaultCommonItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultLoginItemTypeHandlers = remember(viewModel) {
                VaultLoginItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultCardItemTypeHandlers = remember(viewModel) {
                VaultCardItemTypeHandlers.create(viewModel = viewModel)
            },
        )
    }
}

@Composable
private fun VaultItemDialogs(
    dialog: VaultItemState.DialogState?,
    onDismissRequest: () -> Unit,
    onSubmitMasterPassword: (String) -> Unit,
) {
    when (dialog) {
        is VaultItemState.DialogState.Generic -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = null,
                message = dialog.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is VaultItemState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(text = dialog.message),
        )

        VaultItemState.DialogState.MasterPasswordDialog -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = onSubmitMasterPassword,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Composable
private fun VaultItemContent(
    viewState: VaultItemState.ViewState,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultLoginItemTypeHandlers: VaultLoginItemTypeHandlers,
    vaultCardItemTypeHandlers: VaultCardItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    when (viewState) {
        is VaultItemState.ViewState.Error -> BitwardenErrorContent(
            message = viewState.message(),
            onTryAgainClick = vaultCommonItemTypeHandlers.onRefreshClick,
            modifier = modifier,
        )

        is VaultItemState.ViewState.Content -> {
            when (viewState.type) {
                is VaultItemState.ViewState.Content.ItemType.Login -> {
                    VaultItemLoginContent(
                        commonState = viewState.common,
                        loginItemState = viewState.type,
                        vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
                        vaultLoginItemTypeHandlers = vaultLoginItemTypeHandlers,
                        modifier = modifier,
                    )
                }

                is VaultItemState.ViewState.Content.ItemType.Card -> {
                    VaultItemCardContent(
                        commonState = viewState.common,
                        cardState = viewState.type,
                        vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
                        vaultCardItemTypeHandlers = vaultCardItemTypeHandlers,
                        modifier = modifier,
                    )
                }

                is VaultItemState.ViewState.Content.ItemType.Identity -> {
                    VaultItemIdentityContent(
                        commonState = viewState.common,
                        identityState = viewState.type,
                        vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
                        modifier = modifier,
                    )
                }

                is VaultItemState.ViewState.Content.ItemType.SecureNote -> {
                    VaultItemSecureNoteContent(
                        commonState = viewState.common,
                        vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
                        modifier = modifier,
                    )
                }
            }
        }

        VaultItemState.ViewState.Loading -> BitwardenLoadingContent(
            modifier = modifier,
        )
    }
}
