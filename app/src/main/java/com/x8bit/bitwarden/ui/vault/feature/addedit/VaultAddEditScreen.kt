package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.widget.Toast
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalExitManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers

/**
 * Top level composable for the vault add item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultAddEditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScanScreen: () -> Unit,
    viewModel: VaultAddEditViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    exitManager: ExitManager = LocalExitManager.current,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultAddEditEvent.NavigateToQrCodeScan -> {
                onNavigateToQrCodeScanScreen()
            }

            is VaultAddEditEvent.NavigateToManualCodeEntry -> {
                onNavigateToManualCodeEntryScreen()
            }

            is VaultAddEditEvent.NavigateToGeneratorModal -> {
                onNavigateToGeneratorModal(event.generatorMode)
            }

            is VaultAddEditEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultAddEditEvent.NavigateToAttachments -> onNavigateToAttachments(event.cipherId)
            is VaultAddEditEvent.NavigateToMoveToOrganization -> {
                onNavigateToMoveToOrganization(event.cipherId, false)
            }

            is VaultAddEditEvent.NavigateToCollections -> {
                onNavigateToMoveToOrganization(event.cipherId, true)
            }

            VaultAddEditEvent.ExitApp -> exitManager.exitApplication()
            VaultAddEditEvent.NavigateBack -> onNavigateBack.invoke()

            is VaultAddEditEvent.NavigateToTooltipUri -> {
                intentManager.launchUri(
                    "https://bitwarden.com/help/managing-items/#protect-individual-items".toUri(),
                )
            }
        }
    }

    val loginItemTypeHandlers = remember(viewModel) {
        VaultAddEditLoginTypeHandlers.create(viewModel = viewModel)
    }

    val commonTypeHandlers = remember(viewModel) {
        VaultAddEditCommonHandlers.create(viewModel = viewModel)
    }

    val identityItemTypeHandlers = remember(viewModel) {
        VaultAddEditIdentityTypeHandlers.create(viewModel = viewModel)
    }

    val cardItemTypeHandlers = remember(viewModel) {
        VaultAddEditCardTypeHandlers.create(viewModel = viewModel)
    }

    val confirmDeleteClickAction = remember(viewModel) {
        { viewModel.trySendAction(VaultAddEditAction.Common.ConfirmDeleteClick) }
    }

    var pendingDeleteCipher by rememberSaveable { mutableStateOf(false) }

    VaultAddEditItemDialogs(
        dialogState = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultAddEditAction.Common.DismissDialog) }
        },
        onAutofillDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultAddEditAction.Common.InitialAutofillDialogDismissed) }
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
            onDismissClick = { pendingDeleteCipher = false },
            onDismissRequest = { pendingDeleteCipher = false },
        )
    }

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
                        { viewModel.trySendAction(VaultAddEditAction.Common.CloseClick) }
                    },
                )
                    .takeIf { state.shouldShowCloseButton },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultAddEditAction.Common.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOfNotNull(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.attachments),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.AttachmentsClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.move_to_organization),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.MoveToOrganizationClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode || state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.collections),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.CollectionsClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode || !state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.delete),
                                onClick = { pendingDeleteCipher = true },
                            )
                                .takeUnless { state.isAddItemMode },
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is VaultAddEditState.ViewState.Content -> {
                VaultAddEditContent(
                    state = viewState,
                    isAddItemMode = state.isAddItemMode,
                    onTypeOptionClicked = remember(viewModel) {
                        { viewModel.trySendAction(VaultAddEditAction.Common.TypeOptionSelect(it)) }
                    },
                    loginItemTypeHandlers = loginItemTypeHandlers,
                    commonTypeHandlers = commonTypeHandlers,
                    permissionsManager = permissionsManager,
                    identityItemTypeHandlers = identityItemTypeHandlers,
                    cardItemTypeHandlers = cardItemTypeHandlers,
                    modifier = Modifier
                        .imePadding()
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            is VaultAddEditState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            VaultAddEditState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun VaultAddEditItemDialogs(
    dialogState: VaultAddEditState.DialogState?,
    onDismissRequest: () -> Unit,
    onAutofillDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is VaultAddEditState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.label),
            )
        }

        is VaultAddEditState.DialogState.Generic -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultAddEditState.DialogState.InitialAutofillPrompt -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = R.string.bitwarden_autofill_service.asText(),
                    message = R.string.bitwarden_autofill_service_alert2.asText(),
                ),
                onDismissRequest = onAutofillDismissRequest,
            )
        }

        null -> Unit
    }
}
