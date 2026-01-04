package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.bitwarden.ui.platform.components.account.util.initials
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.BitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenOverwriteCredentialConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenPinDialog
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalCredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.components.VaultItemSelectionDialog
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers.VaultItemListingHandlers
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers.VaultItemListingUserVerificationHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the vault item listing screen.
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun VaultItemListingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToAddFolder: (selectedFolderId: String?) -> Unit,
    onNavigateToAddEditSendItem: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSendItem: (route: ViewSendRoute) -> Unit,
    onNavigateToSearch: (searchType: SearchType) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    exitManager: ExitManager = LocalExitManager.current,
    credentialProviderCompletionManager: CredentialProviderCompletionManager =
        LocalCredentialProviderCompletionManager.current,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    viewModel: VaultItemListingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val userVerificationHandlers = remember(viewModel) {
        VaultItemListingUserVerificationHandlers.create(viewModel = viewModel)
    }

    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.RefreshPull) }
        },
    )
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultItemListingEvent.NavigateBack -> onNavigateBack()

            is VaultItemListingEvent.NavigateToVaultItem -> {
                onNavigateToVaultItemScreen(VaultItemArgs(event.id, event.type))
            }

            is VaultItemListingEvent.ShowShareSheet -> {
                intentManager.shareText(event.content)
            }

            is VaultItemListingEvent.NavigateToAddVaultItem -> {
                onNavigateToVaultAddItemScreen(
                    VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.AddItem,
                        vaultItemCipherType = event.vaultItemCipherType,
                        selectedFolderId = event.selectedFolderId,
                        selectedCollectionId = event.selectedCollectionId,
                    ),
                )
            }

            is VaultItemListingEvent.NavigateToViewSendItem -> {
                onNavigateToViewSendItem(
                    ViewSendRoute(sendId = event.id, sendType = event.sendType),
                )
            }

            is VaultItemListingEvent.NavigateToEditCipher -> {
                onNavigateToVaultEditItemScreen(
                    VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = event.cipherId),
                        vaultItemCipherType = event.cipherType,
                    ),
                )
            }

            is VaultItemListingEvent.NavigateToUrl -> {
                intentManager.launchUri(event.url.toUri())
            }

            is VaultItemListingEvent.NavigateToAddSendItem -> {
                onNavigateToAddEditSendItem(
                    AddEditSendRoute(
                        sendType = event.sendType,
                        modeType = ModeType.ADD,
                    ),
                )
            }

            is VaultItemListingEvent.NavigateToEditSendItem -> {
                onNavigateToAddEditSendItem(
                    AddEditSendRoute(
                        sendType = event.sendType,
                        modeType = ModeType.EDIT,
                        sendId = event.id,
                    ),
                )
            }

            is VaultItemListingEvent.NavigateToSearchScreen -> {
                onNavigateToSearch(event.searchType)
            }

            is VaultItemListingEvent.NavigateToFolderItem -> {
                onNavigateToVaultItemListing(VaultItemListingType.Folder(event.folderId))
            }

            is VaultItemListingEvent.NavigateToCollectionItem -> {
                onNavigateToVaultItemListing(VaultItemListingType.Collection(event.collectionId))
            }

            is VaultItemListingEvent.CompleteCredentialRegistration -> {
                credentialProviderCompletionManager.completeCredentialRegistration(event.result)
            }

            is VaultItemListingEvent.CredentialManagerUserVerification -> {
                biometricsManager.promptUserVerification(
                    onSuccess = {
                        userVerificationHandlers
                            .onUserVerificationSuccess(event.selectedCipherView)
                    },
                    onCancel = userVerificationHandlers.onUserVerificationCancelled,
                    onLockOut = userVerificationHandlers.onUserVerificationLockOut,
                    onError = userVerificationHandlers.onUserVerificationFail,
                    onNotSupported = {
                        userVerificationHandlers.onUserVerificationNotSupported(
                            event.selectedCipherView.id,
                        )
                    },
                )
            }

            is VaultItemListingEvent.CompleteFido2Assertion -> {
                credentialProviderCompletionManager.completeFido2Assertion(event.result)
            }

            is VaultItemListingEvent.CompleteProviderGetCredentialsRequest -> {
                credentialProviderCompletionManager
                    .completeProviderGetCredentialsRequest(event.result)
            }

            is VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest -> {
                credentialProviderCompletionManager.completePasswordGet(event.result)
            }

            VaultItemListingEvent.ExitApp -> exitManager.exitApplication()

            is VaultItemListingEvent.NavigateToAddFolder -> {
                onNavigateToAddFolder(event.parentFolderName)
            }

            is VaultItemListingEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    VaultItemListingDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick) }
        },
        onDismissCredentialManagerErrorDialog = remember(viewModel) {
            { errorMessage ->
                viewModel.trySendAction(
                    VaultItemListingsAction
                        .DismissCredentialManagerErrorDialogClick(
                            message = errorMessage,
                        ),
                )
            }
        },
        onConfirmOverwriteExistingPasskey = remember(viewModel) {
            { cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                        cipherViewId = cipherId,
                    ),
                )
            }
        },
        onSubmitMasterPasswordCredentialVerification = remember(viewModel) {
            { password, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                        password = password,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryGetCredentialPasswordVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick(it),
                )
            }
        },
        onSubmitPinCredentialVerification = remember(viewModel) {
            { pin, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.PinUserVerificationSubmit(
                        pin = pin,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryPinCredentialVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.RetryUserVerificationPinVerificationClick(it),
                )
            }
        },
        onSubmitPinSetUpCredentialVerification = remember(viewModel) {
            { pin, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.UserVerificationPinSetUpSubmit(
                        pin = pin,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryPinSetUpCredentialVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.UserVerificationPinSetUpRetryClick(it),
                )
            }
        },
        onDismissUserVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.DismissUserVerificationDialogClick,
                )
            }
        },
        onVaultItemTypeSelected = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.ItemTypeToAddSelected(itemType = it),
                )
            }
        },
        onTrustPrivilegedAppClick = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.TrustPrivilegedAppClick(it),
                )
            }
        },
        onShareCipherDecryptionErrorClick = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.ShareCipherDecryptionErrorClick(it),
                )
            }
        },
    )

    val vaultItemListingHandlers = remember(viewModel) {
        VaultItemListingHandlers.create(viewModel)
    }

    BackHandler(onBack = vaultItemListingHandlers.backClick)
    VaultItemListingScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        pullToRefreshState = pullToRefreshState,
        vaultItemListingHandlers = vaultItemListingHandlers,
    )
}

@Suppress("LongMethod")
@Composable
private fun VaultItemListingDialogs(
    dialogState: VaultItemListingState.DialogState?,
    onDismissRequest: () -> Unit,
    onDismissCredentialManagerErrorDialog: (Text) -> Unit,
    onConfirmOverwriteExistingPasskey: (cipherViewId: String) -> Unit,
    onSubmitMasterPasswordCredentialVerification: (password: String, cipherId: String) -> Unit,
    onRetryGetCredentialPasswordVerification: (cipherId: String) -> Unit,
    onSubmitPinCredentialVerification: (pin: String, cipherId: String) -> Unit,
    onRetryPinCredentialVerification: (cipherViewId: String) -> Unit,
    onSubmitPinSetUpCredentialVerification: (pin: String, cipherId: String) -> Unit,
    onRetryPinSetUpCredentialVerification: (cipherId: String) -> Unit,
    onDismissUserVerification: () -> Unit,
    onVaultItemTypeSelected: (CreateVaultItemType) -> Unit,
    onTrustPrivilegedAppClick: (selectedCipherId: String?) -> Unit,
    onShareCipherDecryptionErrorClick: (selectedCipherId: String) -> Unit,
) {
    when (dialogState) {
        is VaultItemListingState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
            throwable = dialogState.throwable,
        )

        is VaultItemListingState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        is VaultItemListingState.DialogState.CipherDecryptionError -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                confirmButtonText = stringResource(BitwardenString.copy_error_report),
                dismissButtonText = stringResource(BitwardenString.close),
                onConfirmClick = {
                    onShareCipherDecryptionErrorClick(dialogState.selectedCipherId)
                },
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultItemListingState.DialogState.CredentialManagerOperationFail -> BitwardenBasicDialog(
            title = dialogState.title(),
            message = dialogState.message(),
            onDismissRequest = { onDismissCredentialManagerErrorDialog(dialogState.message) },
        )

        is VaultItemListingState.DialogState.OverwritePasskeyConfirmationPrompt -> {
            @Suppress("MaxLineLength")
            BitwardenOverwriteCredentialConfirmationDialog(
                title = stringResource(id = BitwardenString.overwrite_passkey),
                message = stringResource(
                    id = BitwardenString
                        .this_item_already_contains_a_passkey_are_you_sure_you_want_to_overwrite_the_current_passkey,
                ),
                onConfirmClick = { onConfirmOverwriteExistingPasskey(dialogState.cipherViewId) },
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultItemListingState.DialogState.UserVerificationMasterPasswordPrompt -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = { password ->
                    onSubmitMasterPasswordCredentialVerification(
                        password,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissUserVerification,
            )
        }

        is VaultItemListingState.DialogState.UserVerificationMasterPasswordError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryGetCredentialPasswordVerification(dialogState.selectedCipherId)
                },
            )
        }

        is VaultItemListingState.DialogState.UserVerificationPinPrompt -> {
            BitwardenPinDialog(
                onConfirmClick = { pin ->
                    onSubmitPinCredentialVerification(
                        pin,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissUserVerification,
            )
        }

        is VaultItemListingState.DialogState.UserVerificationPinError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryPinCredentialVerification(dialogState.selectedCipherId)
                },
            )
        }

        is VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt -> {
            PinInputDialog(
                onCancelClick = onDismissUserVerification,
                onSubmitClick = { pin ->
                    onSubmitPinSetUpCredentialVerification(pin, dialogState.selectedCipherId)
                },
                onDismissRequest = onDismissUserVerification,
            )
        }

        is VaultItemListingState.DialogState.UserVerificationPinSetUpError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryPinSetUpCredentialVerification(dialogState.selectedCipherId)
                },
            )
        }

        is VaultItemListingState.DialogState.VaultItemTypeSelection -> {
            VaultItemSelectionDialog(
                onDismissRequest = onDismissRequest,
                onOptionSelected = onVaultItemTypeSelected,
                excludedOptions = dialogState.excludedOptions,
            )
        }

        is VaultItemListingState.DialogState.TrustPrivilegedAddPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.unrecognized_browser),
                message = dialogState.message.invoke(),
                confirmButtonText = stringResource(BitwardenString.trust),
                dismissButtonText = stringResource(BitwardenString.cancel),
                onConfirmClick = {
                    onTrustPrivilegedAppClick(dialogState.selectedCipherId)
                },
                onDismissClick = {
                    onDismissCredentialManagerErrorDialog(
                        BitwardenString.passkey_operation_failed_because_the_browser_is_not_trusted
                            .asText(),
                    )
                },
                onDismissRequest = {
                    onDismissCredentialManagerErrorDialog(
                        BitwardenString.passkey_operation_failed_because_the_browser_is_not_trusted
                            .asText(),
                    )
                },
            )
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun VaultItemListingScaffold(
    state: VaultItemListingState,
    pullToRefreshState: BitwardenPullToRefreshState,
    snackbarHostState: BitwardenSnackbarHostState,
    vaultItemListingHandlers: VaultItemListingHandlers,
) {
    var isAccountMenuVisible by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.appBarTitle(),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = vaultItemListingHandlers.backClick,
                )
                    .takeIf { state.shouldShowNavigationIcon },
                actions = {
                    if (state.shouldShowAccountSwitcher) {
                        BitwardenAccountActionItem(
                            initials = state.activeAccountSummary.initials,
                            color = state.activeAccountSummary.avatarColor,
                            onClick = { isAccountMenuVisible = !isAccountMenuVisible },
                        )
                    }
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = BitwardenString.search_vault),
                        onClick = vaultItemListingHandlers.searchIconClick,
                    )
                    if (state.shouldShowOverflowMenu) {
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(BitwardenString.more),
                            menuItemDataList = persistentListOf(
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.sync),
                                    onClick = vaultItemListingHandlers.syncClick,
                                ),
                                OverflowMenuItemData(
                                    text = stringResource(id = BitwardenString.lock),
                                    onClick = vaultItemListingHandlers.lockClick,
                                ),
                            ),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.hasAddItemFabButton) {
                BitwardenFloatingActionButton(
                    onClick = vaultItemListingHandlers.addVaultItemClick,
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_plus_large),
                    contentDescription = stringResource(id = BitwardenString.add_item),
                    modifier = Modifier.testTag(tag = "AddItemButton"),
                )
            }
        },
        overlay = {
            BitwardenAccountSwitcher(
                isVisible = isAccountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = vaultItemListingHandlers.switchAccountClick,
                onLockAccountClick = vaultItemListingHandlers.lockAccountClick,
                onLogoutAccountClick = vaultItemListingHandlers.logoutAccountClick,
                onAddAccountClick = {
                    // Not available
                },
                onDismissRequest = { isAccountMenuVisible = false },
                isAddAccountAvailable = false,
                topAppBarScrollBehavior = scrollBehavior,
                modifier = Modifier.fillMaxSize(),
            )
        },
        pullToRefreshState = pullToRefreshState,
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        when (state.viewState) {
            is VaultItemListingState.ViewState.Content -> {
                VaultItemListingContent(
                    state = state.viewState,
                    showAddTotpBanner = state.isTotp,
                    policyDisablesSend = state.policyDisablesSend &&
                        state.itemListingType is VaultItemListingState.ItemListingType.Send,
                    vaultItemClick = vaultItemListingHandlers.itemClick,
                    collectionClick = vaultItemListingHandlers.collectionClick,
                    folderClick = vaultItemListingHandlers.folderClick,
                    masterPasswordRepromptSubmit = vaultItemListingHandlers
                        .masterPasswordRepromptSubmit,
                    onOverflowItemClick = vaultItemListingHandlers.overflowItemClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VaultItemListingState.ViewState.NoItems -> {
                VaultItemListingEmpty(
                    state = state.viewState,
                    policyDisablesSend = state.policyDisablesSend &&
                        state.itemListingType is VaultItemListingState.ItemListingType.Send,
                    addItemClickAction = vaultItemListingHandlers.addVaultItemClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VaultItemListingState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = state.viewState.message(),
                    onTryAgainClick = vaultItemListingHandlers.refreshClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VaultItemListingState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
