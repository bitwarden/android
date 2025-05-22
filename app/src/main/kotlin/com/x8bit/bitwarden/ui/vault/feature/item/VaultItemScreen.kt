package com.x8bit.bitwarden.ui.vault.feature.item

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCardItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultIdentityItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultLoginItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultSshKeyItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType

/**
 * Displays the vault item screen.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemScreen(
    viewModel: VaultItemViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
    onNavigateToVaultAddEditItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
    onNavigateToPasswordHistory: (vaultItemId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

    val fileChooserLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)
            ?.let {
                viewModel.trySendAction(
                    VaultItemAction.Common.AttachmentFileLocationReceive(it.uri),
                )
            }
            ?: viewModel.trySendAction(VaultItemAction.Common.NoAttachmentFileLocationReceive)
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultItemEvent.NavigateBack -> onNavigateBack()

            is VaultItemEvent.NavigateToAddEdit -> {
                onNavigateToVaultAddEditItem(
                    VaultAddEditArgs(
                        vaultAddEditType = if (event.isClone) {
                            VaultAddEditType.CloneItem(vaultItemId = event.itemId)
                        } else {
                            VaultAddEditType.EditItem(vaultItemId = event.itemId)
                        },
                        vaultItemCipherType = event.type,
                    ),
                )
            }

            is VaultItemEvent.NavigateToPasswordHistory -> {
                onNavigateToPasswordHistory(event.itemId)
            }

            is VaultItemEvent.NavigateToUri -> intentManager.launchUri(event.uri.toUri())

            is VaultItemEvent.NavigateToAttachments -> onNavigateToAttachments(event.itemId)

            is VaultItemEvent.NavigateToMoveToOrganization -> {
                onNavigateToMoveToOrganization(event.itemId, false)
            }

            is VaultItemEvent.NavigateToCollections -> {
                onNavigateToMoveToOrganization(event.itemId, true)
            }

            is VaultItemEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultItemEvent.NavigateToSelectAttachmentSaveLocation -> {
                fileChooserLauncher.launch(
                    intentManager.createDocumentIntent(event.fileName),
                )
            }
        }
    }

    VaultItemDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick) }
        },
        onSubmitMasterPassword = remember(viewModel) {
            { masterPassword, action ->
                viewModel.trySendAction(
                    VaultItemAction.Common.MasterPasswordSubmit(
                        masterPassword = masterPassword,
                        action = action,
                    ),
                )
            }
        },
        onConfirmDeleteClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick) }
        },
        onConfirmCloneWithoutFido2Credential = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemAction.Common.ConfirmCloneWithoutFido2CredentialClick,
                )
            }
        },
        onConfirmRestoreAction = remember(viewModel) {
            { viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.title(),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultItemAction.Common.CloseClick) }
                },
                actions = {
                    if (state.canRestore) {
                        BitwardenTextButton(
                            label = stringResource(id = R.string.restore),
                            onClick = remember(viewModel) {
                                {
                                    viewModel.trySendAction(
                                        VaultItemAction.Common.RestoreVaultItemClick,
                                    )
                                }
                            },
                            modifier = Modifier.testTag("RestoreButton"),
                        )
                    }
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
                                .takeIf {
                                    state.isCipherInCollection &&
                                        state.canAssignToCollections
                                },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.delete),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultItemAction.Common.DeleteClick,
                                        )
                                    }
                                },
                            )
                                .takeIf { state.canDelete },
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
                BitwardenFloatingActionButton(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultItemAction.Common.EditClick) }
                    },
                    painter = rememberVectorPainter(id = R.drawable.ic_pencil),
                    contentDescription = stringResource(id = R.string.edit_item),
                    modifier = Modifier
                        .testTag(tag = "EditItemButton")
                        .padding(bottom = 16.dp),
                )
            }
        },
    ) {
        VaultItemContent(
            viewState = state.viewState,
            modifier = Modifier
                .fillMaxSize(),
            vaultCommonItemTypeHandlers = remember(viewModel) {
                VaultCommonItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultLoginItemTypeHandlers = remember(viewModel) {
                VaultLoginItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultCardItemTypeHandlers = remember(viewModel) {
                VaultCardItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultSshKeyItemTypeHandlers = remember(viewModel) {
                VaultSshKeyItemTypeHandlers.create(viewModel = viewModel)
            },
            vaultIdentityItemTypeHandlers = remember(viewModel) {
                VaultIdentityItemTypeHandlers.create(viewModel = viewModel)
            },
        )
    }
}

@Composable
private fun VaultItemDialogs(
    dialog: VaultItemState.DialogState?,
    onDismissRequest: () -> Unit,
    onConfirmDeleteClick: () -> Unit,
    onSubmitMasterPassword: (masterPassword: String, action: PasswordRepromptAction) -> Unit,
    onConfirmCloneWithoutFido2Credential: () -> Unit,
    onConfirmRestoreAction: () -> Unit,
) {
    when (dialog) {
        is VaultItemState.DialogState.Generic -> BitwardenBasicDialog(
            title = null,
            message = dialog.message(),
            throwable = dialog.error,
            onDismissRequest = onDismissRequest,
        )

        is VaultItemState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialog.message(),
        )

        is VaultItemState.DialogState.MasterPasswordDialog -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = { onSubmitMasterPassword(it, dialog.action) },
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultItemState.DialogState.DeleteConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.delete),
                message = dialog.message.invoke(),
                confirmButtonText = stringResource(id = R.string.ok),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = onConfirmDeleteClick,
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultItemState.DialogState.Fido2CredentialCannotBeCopiedConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.passkey_will_not_be_copied),
                message = dialog.message.invoke(),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.no),
                onConfirmClick = onConfirmCloneWithoutFido2Credential,
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest,
            )
        }

        VaultItemState.DialogState.RestoreItemDialog -> BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.restore),
            message = stringResource(id = R.string.do_you_really_want_to_restore_cipher),
            confirmButtonText = stringResource(id = R.string.ok),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = onConfirmRestoreAction,
            onDismissClick = onDismissRequest,
            onDismissRequest = onDismissRequest,
        )

        null -> Unit
    }
}

@Composable
private fun VaultItemContent(
    viewState: VaultItemState.ViewState,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultLoginItemTypeHandlers: VaultLoginItemTypeHandlers,
    vaultCardItemTypeHandlers: VaultCardItemTypeHandlers,
    vaultSshKeyItemTypeHandlers: VaultSshKeyItemTypeHandlers,
    vaultIdentityItemTypeHandlers: VaultIdentityItemTypeHandlers,
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
                        vaultIdentityItemTypeHandlers = vaultIdentityItemTypeHandlers,
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

                is VaultItemState.ViewState.Content.ItemType.SshKey -> {
                    VaultItemSshKeyContent(
                        commonState = viewState.common,
                        sshKeyItemState = viewState.type,
                        vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
                        vaultSshKeyItemTypeHandlers = vaultSshKeyItemTypeHandlers,
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
