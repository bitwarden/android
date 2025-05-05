package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.coachmark.CoachMarkContainer
import com.x8bit.bitwarden.ui.platform.components.coachmark.rememberLazyListCoachMarkState
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenOverwritePasskeyConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenPinDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.radio.BitwardenRadioButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalExitManager
import com.x8bit.bitwarden.ui.platform.composition.LocalFido2CompletionManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
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
    fido2CompletionManager: Fido2CompletionManager = LocalFido2CompletionManager.current,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    val userVerificationHandlers = remember(viewModel) {
        VaultAddEditUserVerificationHandlers.create(viewModel = viewModel)
    }

    val lazyListState = rememberLazyListState()
    val coachMarkState = rememberLazyListCoachMarkState(
        lazyListState = lazyListState,
        orderedList = AddEditItemCoachMark.entries,
    )
    val scope = rememberCoroutineScope()
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

            is VaultAddEditEvent.NavigateToAuthenticatorKeyTooltipUri -> {
                intentManager.launchUri(
                    "https://bitwarden.com/help/integrated-authenticator".toUri(),
                )
            }

            is VaultAddEditEvent.CompleteFido2Registration -> {
                fido2CompletionManager.completeFido2Registration(
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
        onFido2ErrorDismiss = remember(viewModel) {
            { errorMessage ->
                viewModel.trySendAction(
                    VaultAddEditAction.Common.Fido2ErrorDialogDismissed(message = errorMessage),
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
                                    .takeUnless {
                                        state.isAddItemMode || state.isCipherInCollection
                                    },
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
                                    .takeUnless {
                                        state.isAddItemMode ||
                                            !state.isCipherInCollection ||
                                            !state.canAssociateToCollections
                                    },
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.delete),
                                    onClick = { pendingDeleteCipher = true },
                                )
                                    .takeUnless { state.isAddItemMode || !state.canDelete },
                            ),
                        )
                    },
                )
            },
        ) {
            when (val viewState = state.viewState) {
                is VaultAddEditState.ViewState.Content -> {
                    VaultAddEditContent(
                        state = viewState,
                        isAddItemMode = state.isAddItemMode,
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
    onFido2ErrorDismiss: (Text) -> Unit,
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
                title = stringResource(id = R.string.bitwarden_autofill_service),
                message = stringResource(id = R.string.bitwarden_autofill_service_alert2),
                onDismissRequest = onAutofillDismissRequest,
            )
        }

        is VaultAddEditState.DialogState.Fido2Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = R.string.an_error_has_occurred),
                message = dialogState.message(),
                onDismissRequest = { onFido2ErrorDismiss(dialogState.message) },
            )
        }

        is VaultAddEditState.DialogState.OverwritePasskeyConfirmationPrompt -> {
            BitwardenOverwritePasskeyConfirmationDialog(
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
                message = stringResource(id = R.string.invalid_master_password),
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
                message = stringResource(id = R.string.invalid_pin),
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
                    id = R.string.validation_field_required,
                    stringResource(id = R.string.pin),
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
        sheetTitle = stringResource(R.string.folders),
        onDismiss = handlers.onDismissBottomSheet,
        topBarActions = { animatedOnDismiss ->
            BitwardenTextButton(
                label = stringResource(R.string.save),
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
                    label = stringResource(R.string.add_folder),
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
                    label = stringResource(id = R.string.add_folder),
                    onClick = {
                        onOptionSelected(addFolderText)
                        inEditMode = true
                    },
                    leadingIcon = painterResource(id = R.drawable.ic_plus_small),
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
        sheetTitle = stringResource(R.string.owner),
        onDismiss = handlers.onDismissBottomSheet,
        topBarActions = { animatedOnDismiss ->
            BitwardenTextButton(
                label = stringResource(R.string.save),
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
