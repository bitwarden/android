package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.coachmark.CoachMarkContainer
import com.bitwarden.ui.platform.components.coachmark.model.rememberLazyListCoachMarkState
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.radio.BitwardenRadioButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenOverwriteCredentialConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenPinDialog
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalCredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditSshKeyTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditUserVerificationHandlers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

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
    credentialProviderCompletionManager: CredentialProviderCompletionManager =
        LocalCredentialProviderCompletionManager.current,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val userVerificationHandlers = remember(viewModel) {
        VaultAddEditUserVerificationHandlers.create(viewModel = viewModel)
    }

    val lazyListState = rememberLazyListState()
    val coachMarkState = rememberLazyListCoachMarkState(
        lazyListState = lazyListState,
        orderedList = AddEditItemCoachMark.entries,
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
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

            is VaultAddEditEvent.NavigateToAuthenticatorKeyTooltipUri -> {
                intentManager.launchUri(
                    "https://bitwarden.com/help/integrated-authenticator".toUri(),
                )
            }

            is VaultAddEditEvent.CompleteCredentialRegistration -> {
                credentialProviderCompletionManager.completeCredentialRegistration(
                    result = event.result,
                )
            }

            is VaultAddEditEvent.Fido2UserVerification -> {
                biometricsManager.promptUserVerification(
                    onSuccess = userVerificationHandlers.onUserVerificationSuccess,
                    onCancel = userVerificationHandlers.onUserVerificationCancelled,
                    onError = userVerificationHandlers.onUserVerificationFail,
                    onLockOut = userVerificationHandlers.onUserVerificationLockOut,
                    onNotSupported = userVerificationHandlers.onUserVerificationNotSupported,
                )
            }

            VaultAddEditEvent.StartAddLoginItemCoachMarkTour -> {
                scope.launch {
                    coachMarkState.showCoachMark(
                        coachMarkToShow = AddEditItemCoachMark.GENERATE_PASSWORD,
                    )
                }
            }

            is VaultAddEditEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)

            VaultAddEditEvent.NavigateToLearnMore -> {
                intentManager.launchUri("https://bitwarden.com/help/uri-match-detection/".toUri())
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

    val sshKeyItemTypeHandlers = remember(viewModel) {
        VaultAddEditSshKeyTypeHandlers.create(viewModel = viewModel)
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
        onCredentialErrorDismiss = remember(viewModel) {
            { errorMessage ->
                viewModel.trySendAction(
                    VaultAddEditAction
                        .Common
                        .CredentialErrorDialogDismissed(
                            message = errorMessage,
                        ),
                )
            }
        },
        onConfirmOverwriteExistingPasskey = remember(viewModel) {
            {
                viewModel.trySendAction(
                    action = VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick,
                )
            }
        },
        onSubmitMasterPasswordFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    action = VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(it),
                )
            }
        },
        onRetryFido2PasswordVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    action = VaultAddEditAction.Common.RetryFido2PasswordVerificationClick,
                )
            }
        },
        onSubmitPinFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultAddEditAction.Common.PinFido2VerificationSubmit(it),
                )
            }
        },
        onRetryFido2PinVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultAddEditAction.Common.RetryFido2PinVerificationClick,
                )
            }
        },
        onSubmitPinSetUpFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultAddEditAction.Common.PinFido2SetUpSubmit(it),
                )
            }
        },
        onRetryPinSetUpFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultAddEditAction.Common.PinFido2SetUpRetryClick,
                )
            }
        },
        onDismissFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
                )
            }
        },
    )

    if (pendingDeleteCipher) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.delete),
            message = stringResource(id = BitwardenString.do_you_really_want_to_soft_delete_cipher),
            confirmButtonText = stringResource(id = BitwardenString.okay),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                pendingDeleteCipher = false
                confirmDeleteClickAction()
            },
            onDismissClick = { pendingDeleteCipher = false },
            onDismissRequest = { pendingDeleteCipher = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val coroutineScope = rememberCoroutineScope()
    val scrollBackToTop: () -> Unit = remember {
        {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
    CoachMarkContainer(
        state = coachMarkState,
    ) {
        BitwardenScaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                BitwardenTopAppBar(
                    title = state.screenDisplayName(),
                    navigationIcon = NavigationIcon(
                        navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                        navigationIconContentDescription = stringResource(
                            id = BitwardenString.close,
                        ),
                        onNavigationIconClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultAddEditAction.Common.CloseClick) }
                        },
                    )
                        .takeIf { state.shouldShowCloseButton },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        BitwardenTextButton(
                            label = stringResource(id = BitwardenString.save),
                            onClick = remember(viewModel) {
                                { viewModel.trySendAction(VaultAddEditAction.Common.SaveClick) }
                            },
                            modifier = Modifier.testTag("SaveButton"),
                        )
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(BitwardenString.more),
                            menuItemDataList = persistentListOfNotNull(
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.attachments),
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
                                    text = stringResource(
                                        id = BitwardenString.move_to_organization,
                                    ),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                VaultAddEditAction.Common.MoveToOrganizationClick,
                                            )
                                        }
                                    },
                                )
                                    .takeUnless { !state.shouldShowMoveToOrganization },
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.collections),
                                    onClick = remember(viewModel) {
                                        {
                                            viewModel.trySendAction(
                                                VaultAddEditAction.Common.CollectionsClick,
                                            )
                                        }
                                    },
                                )
                                    .takeUnless {
                                        state.isAddItemMode ||
                                            !state.isCipherInCollection ||
                                            !state.canAssociateToCollections
                                    },
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.delete),
                                    onClick = { pendingDeleteCipher = true },
                                )
                                    .takeUnless { state.isAddItemMode || !state.canDelete },
                            ),
                        )
                    },
                )
            },
            snackbarHost = {
                BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
            },
        ) {
            when (val viewState = state.viewState) {
                is VaultAddEditState.ViewState.Content -> {
                    VaultAddEditContent(
                        state = viewState,
                        isAddItemMode = state.isAddItemMode,
                        defaultUriMatchType = state.defaultUriMatchType,
                        loginItemTypeHandlers = loginItemTypeHandlers,
                        commonTypeHandlers = commonTypeHandlers,
                        permissionsManager = permissionsManager,
                        identityItemTypeHandlers = identityItemTypeHandlers,
                        cardItemTypeHandlers = cardItemTypeHandlers,
                        sshKeyItemTypeHandlers = sshKeyItemTypeHandlers,
                        lazyListState = lazyListState,
                        onPreviousCoachMark = {
                            coroutineScope.launch {
                                coachMarkState.showPreviousCoachMark()
                            }
                        },
                        onNextCoachMark = {
                            coroutineScope.launch {
                                coachMarkState.showNextCoachMark()
                            }
                        },
                        onCoachMarkTourComplete = {
                            coachMarkState.coachingComplete(onComplete = scrollBackToTop)
                        },
                        onCoachMarkDismissed = scrollBackToTop,
                        shouldShowLearnAboutLoginsCard = state.shouldShowLearnAboutNewLogins,
                        modifier = Modifier
                            .fillMaxSize(),
                    )

                    BottomSheetViews(
                        bottomSheetState = state.bottomSheetState,
                        viewState = viewState.common,
                        handlers = commonTypeHandlers,
                    )
                }

                is VaultAddEditState.ViewState.Error -> {
                    BitwardenErrorContent(
                        message = viewState.message(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                VaultAddEditState.ViewState.Loading -> {
                    BitwardenLoadingContent(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun VaultAddEditItemDialogs(
    dialogState: VaultAddEditState.DialogState?,
    onDismissRequest: () -> Unit,
    onAutofillDismissRequest: () -> Unit,
    onCredentialErrorDismiss: (Text) -> Unit,
    onConfirmOverwriteExistingPasskey: () -> Unit,
    onSubmitMasterPasswordFido2Verification: (password: String) -> Unit,
    onRetryFido2PasswordVerification: () -> Unit,
    onSubmitPinFido2Verification: (pin: String) -> Unit,
    onRetryFido2PinVerification: () -> Unit,
    onSubmitPinSetUpFido2Verification: (pin: String) -> Unit,
    onRetryPinSetUpFido2Verification: () -> Unit,
    onDismissFido2Verification: () -> Unit,
) {
    when (dialogState) {
        is VaultAddEditState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.label())
        }

        is VaultAddEditState.DialogState.Generic -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                throwable = dialogState.error,
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultAddEditState.DialogState.InitialAutofillPrompt -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.bitwarden_autofill_service),
                message = stringResource(id = BitwardenString.bitwarden_autofill_service_alert2),
                onDismissRequest = onAutofillDismissRequest,
            )
        }

        is VaultAddEditState.DialogState.CredentialError -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialogState.message(),
                onDismissRequest = { onCredentialErrorDismiss(dialogState.message) },
            )
        }

        is VaultAddEditState.DialogState.OverwritePasskeyConfirmationPrompt -> {
            @Suppress("MaxLineLength")
            BitwardenOverwriteCredentialConfirmationDialog(
                title = stringResource(id = BitwardenString.overwrite_passkey),
                message = stringResource(
                    id = BitwardenString
                        .this_item_already_contains_a_passkey_are_you_sure_you_want_to_overwrite_the_current_passkey,
                ),
                onConfirmClick = onConfirmOverwriteExistingPasskey,
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultAddEditState.DialogState.Fido2MasterPasswordPrompt -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = onSubmitMasterPasswordFido2Verification,
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultAddEditState.DialogState.Fido2MasterPasswordError -> {
            BitwardenBasicDialog(
                title = null,
                message = stringResource(id = BitwardenString.invalid_master_password),
                onDismissRequest = onRetryFido2PasswordVerification,
            )
        }

        is VaultAddEditState.DialogState.Fido2PinPrompt -> {
            BitwardenPinDialog(
                onConfirmClick = onSubmitPinFido2Verification,
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultAddEditState.DialogState.Fido2PinError -> {
            BitwardenBasicDialog(
                title = null,
                message = stringResource(id = BitwardenString.invalid_pin),
                onDismissRequest = onRetryFido2PinVerification,
            )
        }

        is VaultAddEditState.DialogState.Fido2PinSetUpPrompt -> {
            PinInputDialog(
                onCancelClick = onDismissFido2Verification,
                onSubmitClick = onSubmitPinSetUpFido2Verification,
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultAddEditState.DialogState.Fido2PinSetUpError -> {
            BitwardenBasicDialog(
                title = null,
                message = stringResource(
                    id = BitwardenString.validation_field_required,
                    stringResource(id = BitwardenString.pin),
                ),
                onDismissRequest = onRetryPinSetUpFido2Verification,
            )
        }

        null -> Unit
    }
}

@Composable
private fun BottomSheetViews(
    bottomSheetState: VaultAddEditState.BottomSheetState?,
    viewState: VaultAddEditState.ViewState.Content.Common,
    handlers: VaultAddEditCommonHandlers,
    modifier: Modifier = Modifier,
) {
    when (bottomSheetState) {
        is VaultAddEditState.BottomSheetState.FolderSelection -> {
            FolderSelectionBottomSheet(
                state = viewState,
                handlers = handlers,
                modifier = modifier,
            )
        }

        is VaultAddEditState.BottomSheetState.OwnerSelection -> {
            OwnerSelectionBottomSheet(
                state = viewState,
                handlers = handlers,
                modifier = modifier,
            )
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSelectionBottomSheet(
    state: VaultAddEditState.ViewState.Content.Common,
    handlers: VaultAddEditCommonHandlers,
    modifier: Modifier = Modifier,
) {
    var selectedOptionState by rememberSaveable {
        mutableStateOf(state.selectedFolder?.name.orEmpty())
    }
    BitwardenModalBottomSheet(
        sheetTitle = stringResource(BitwardenString.folders),
        onDismiss = handlers.onDismissBottomSheet,
        topBarActions = { animatedOnDismiss ->
            BitwardenTextButton(
                label = stringResource(BitwardenString.save),
                onClick = {
                    handlers.onDismissBottomSheet()
                    state
                        .availableFolders
                        .firstOrNull {
                            it.name == selectedOptionState
                        }
                        ?.run {
                            handlers.onChangeToExistingFolder(this.id)
                        }
                        ?: run {
                            handlers.onOnAddFolder(selectedOptionState)
                        }
                    animatedOnDismiss()
                },
                isEnabled = selectedOptionState.isNotBlank(),
            )
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier.statusBarsPadding(),
    ) {
        FolderSelectionBottomSheetContent(
            options = state.availableFolders.map { it.name }.toImmutableList(),
            selectedOption = selectedOptionState,
            onOptionSelected = {
                selectedOptionState = it
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun FolderSelectionBottomSheetContent(
    options: ImmutableList<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .standardHorizontalMargin(),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        itemsIndexed(options) { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardStyle(
                        cardStyle = if (index == 0) {
                            CardStyle.Top()
                        } else {
                            CardStyle.Middle()
                        },
                        onClick = {
                            onOptionSelected(option)
                        },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option,
                    color = BitwardenTheme.colorScheme.text.primary,
                    style = BitwardenTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                )
                BitwardenRadioButton(
                    isSelected = selectedOption == option,
                    onClick = {
                        onOptionSelected(option)
                    },
                )
            }
        }
        item {
            var inEditMode by rememberSaveable {
                mutableStateOf(false)
            }
            var addFolderText by rememberSaveable {
                mutableStateOf("")
            }
            val cardStyle = if (options.isEmpty()) CardStyle.Full else CardStyle.Bottom
            if (inEditMode) {
                BitwardenTextField(
                    label = stringResource(BitwardenString.add_folder),
                    value = addFolderText,
                    onValueChange = {
                        addFolderText = it
                        onOptionSelected(it)
                    },
                    autoFocus = true,
                    cardStyle = cardStyle,
                    modifier = Modifier
                        .fillMaxWidth(),
                    actions = {
                        BitwardenRadioButton(
                            isSelected = selectedOption == addFolderText,
                            onClick = {
                                onOptionSelected(addFolderText)
                            },
                        )
                    },
                )
            } else {
                BitwardenClickableText(
                    label = stringResource(id = BitwardenString.add_folder),
                    onClick = {
                        onOptionSelected(addFolderText)
                        inEditMode = true
                    },
                    leadingIcon = painterResource(id = BitwardenDrawable.ic_plus_small),
                    style = BitwardenTheme.typography.labelMedium,
                    innerPadding = PaddingValues(all = 16.dp),
                    cornerSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardStyle(cardStyle = cardStyle, paddingVertical = 0.dp),
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerSelectionBottomSheet(
    state: VaultAddEditState.ViewState.Content.Common,
    handlers: VaultAddEditCommonHandlers,
    modifier: Modifier = Modifier,
) {

    var selectedOptionState by rememberSaveable {
        mutableStateOf(state.selectedOwner?.name.orEmpty())
    }
    BitwardenModalBottomSheet(
        sheetTitle = stringResource(BitwardenString.owner),
        onDismiss = handlers.onDismissBottomSheet,
        topBarActions = { animatedOnDismiss ->
            BitwardenTextButton(
                label = stringResource(BitwardenString.save),
                onClick = {
                    handlers.onDismissBottomSheet()
                    state
                        .availableOwners
                        .firstOrNull {
                            it.name == selectedOptionState
                        }
                        ?.run {
                            handlers.onOwnerSelected(this.id)
                        }
                    animatedOnDismiss()
                },
                isEnabled = selectedOptionState.isNotBlank(),
            )
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier.statusBarsPadding(),
    ) {
        OwnerSelectionBottomSheetContent(
            options = state.availableOwners.map { it.name }.toImmutableList(),
            selectedOption = selectedOptionState,
            onOptionSelected = {
                selectedOptionState = it
            },
        )
    }
}

@Composable
private fun OwnerSelectionBottomSheetContent(
    options: ImmutableList<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .standardHorizontalMargin(),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        itemsIndexed(options) { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardStyle(
                        cardStyle = options.toListItemCardStyle(index = index),
                        onClick = {
                            onOptionSelected(option)
                        },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option,
                    color = BitwardenTheme.colorScheme.text.primary,
                    style = BitwardenTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                BitwardenRadioButton(
                    isSelected = selectedOption == option,
                    onClick = {
                        onOptionSelected(option)
                    },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
