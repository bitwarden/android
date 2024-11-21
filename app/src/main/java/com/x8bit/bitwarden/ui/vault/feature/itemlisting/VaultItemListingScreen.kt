package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenOverwritePasskeyConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenPinDialog
import com.x8bit.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.scaffold.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalExitManager
import com.x8bit.bitwarden.ui.platform.composition.LocalFido2CompletionManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers.VaultItemListingHandlers
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers.VaultItemListingUserVerificationHandlers
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
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
    onNavigateToVaultItem: (id: String) -> Unit,
    onNavigateToVaultEditItemScreen: (cipherVaultId: String) -> Unit,
    onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    onNavigateToVaultAddItemScreen: (
        vaultItemCipherType: VaultItemCipherType,
        selectedFolderId: String?,
        selectedCollectionId: String?,
    ) -> Unit,
    onNavigateToAddSendItem: () -> Unit,
    onNavigateToEditSendItem: (sendId: String) -> Unit,
    onNavigateToSearch: (searchType: SearchType) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    exitManager: ExitManager = LocalExitManager.current,
    fido2CompletionManager: Fido2CompletionManager = LocalFido2CompletionManager.current,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    viewModel: VaultItemListingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
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
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultItemListingEvent.NavigateBack -> onNavigateBack()

            is VaultItemListingEvent.NavigateToVaultItem -> {
                onNavigateToVaultItem(event.id)
            }

            is VaultItemListingEvent.ShowShareSheet -> {
                intentManager.shareText(event.content)
            }

            is VaultItemListingEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultItemListingEvent.NavigateToAddVaultItem -> {
                onNavigateToVaultAddItemScreen(
                    event.vaultItemCipherType,
                    event.selectedFolderId,
                    event.selectedCollectionId,
                )
            }

            is VaultItemListingEvent.NavigateToEditCipher -> {
                onNavigateToVaultEditItemScreen(event.cipherId)
            }

            is VaultItemListingEvent.NavigateToUrl -> {
                intentManager.launchUri(event.url.toUri())
            }

            is VaultItemListingEvent.NavigateToAddSendItem -> {
                onNavigateToAddSendItem()
            }

            is VaultItemListingEvent.NavigateToSendItem -> {
                onNavigateToEditSendItem(event.id)
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

            is VaultItemListingEvent.CompleteFido2Registration -> {
                fido2CompletionManager.completeFido2Registration(event.result)
            }

            is VaultItemListingEvent.Fido2UserVerification -> {
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
                fido2CompletionManager.completeFido2Assertion(event.result)
            }

            is VaultItemListingEvent.CompleteFido2GetCredentialsRequest -> {
                fido2CompletionManager.completeFido2GetCredentialRequest(event.result)
            }

            VaultItemListingEvent.ExitApp -> exitManager.exitApplication()
        }
    }

    VaultItemListingDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick) }
        },
        onDismissFido2ErrorDialog = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.DismissFido2ErrorDialogClick,
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
        onSubmitMasterPasswordFido2Verification = remember(viewModel) {
            { password, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.MasterPasswordFido2VerificationSubmit(
                        password = password,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryFido2PasswordVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.RetryFido2PasswordVerificationClick(it),
                )
            }
        },
        onSubmitPinFido2Verification = remember(viewModel) {
            { pin, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.PinFido2VerificationSubmit(
                        pin = pin,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryFido2PinVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.RetryFido2PinVerificationClick(it),
                )
            }
        },
        onSubmitPinSetUpFido2Verification = remember(viewModel) {
            { pin, cipherId ->
                viewModel.trySendAction(
                    VaultItemListingsAction.PinFido2SetUpSubmit(
                        pin = pin,
                        selectedCipherId = cipherId,
                    ),
                )
            }
        },
        onRetryPinSetUpFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.PinFido2SetUpRetryClick(it),
                )
            }
        },
        onDismissFido2Verification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.DismissFido2VerificationDialogClick,
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
        pullToRefreshState = pullToRefreshState,
        vaultItemListingHandlers = vaultItemListingHandlers,
    )
}

@Suppress("LongMethod")
@Composable
private fun VaultItemListingDialogs(
    dialogState: VaultItemListingState.DialogState?,
    onDismissRequest: () -> Unit,
    onDismissFido2ErrorDialog: () -> Unit,
    onConfirmOverwriteExistingPasskey: (cipherViewId: String) -> Unit,
    onSubmitMasterPasswordFido2Verification: (password: String, cipherId: String) -> Unit,
    onRetryFido2PasswordVerification: (cipherId: String) -> Unit,
    onSubmitPinFido2Verification: (pin: String, cipherId: String) -> Unit,
    onRetryFido2PinVerification: (cipherViewId: String) -> Unit,
    onSubmitPinSetUpFido2Verification: (pin: String, cipherId: String) -> Unit,
    onRetryPinSetUpFido2Verification: (cipherId: String) -> Unit,
    onDismissFido2Verification: () -> Unit,
) {
    when (dialogState) {
        is VaultItemListingState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
        )

        is VaultItemListingState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        is VaultItemListingState.DialogState.Fido2OperationFail -> BitwardenBasicDialog(
            title = dialogState.title(),
            message = dialogState.message(),
            onDismissRequest = onDismissFido2ErrorDialog,
        )

        is VaultItemListingState.DialogState.OverwritePasskeyConfirmationPrompt -> {
            BitwardenOverwritePasskeyConfirmationDialog(
                onConfirmClick = { onConfirmOverwriteExistingPasskey(dialogState.cipherViewId) },
                onDismissRequest = onDismissRequest,
            )
        }

        is VaultItemListingState.DialogState.Fido2MasterPasswordPrompt -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = { password ->
                    onSubmitMasterPasswordFido2Verification(
                        password,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultItemListingState.DialogState.Fido2MasterPasswordError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryFido2PasswordVerification(dialogState.selectedCipherId)
                },
            )
        }

        is VaultItemListingState.DialogState.Fido2PinPrompt -> {
            BitwardenPinDialog(
                onConfirmClick = { pin ->
                    onSubmitPinFido2Verification(
                        pin,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultItemListingState.DialogState.Fido2PinError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryFido2PinVerification(dialogState.selectedCipherId)
                },
            )
        }

        is VaultItemListingState.DialogState.Fido2PinSetUpPrompt -> {
            PinInputDialog(
                onCancelClick = onDismissFido2Verification,
                onSubmitClick = { pin ->
                    onSubmitPinSetUpFido2Verification(pin, dialogState.selectedCipherId)
                },
                onDismissRequest = onDismissFido2Verification,
            )
        }

        is VaultItemListingState.DialogState.Fido2PinSetUpError -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = {
                    onRetryPinSetUpFido2Verification(dialogState.selectedCipherId)
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
                    navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                    navigationIconContentDescription = stringResource(id = R.string.back),
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
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = vaultItemListingHandlers.searchIconClick,
                    )
                    if (state.shouldShowOverflowMenu) {
                        BitwardenOverflowActionItem(
                            menuItemDataList = persistentListOf(
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.sync),
                                    onClick = vaultItemListingHandlers.syncClick,
                                ),
                                OverflowMenuItemData(
                                    text = stringResource(id = R.string.lock),
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
                    painter = rememberVectorPainter(id = R.drawable.ic_plus_large),
                    contentDescription = stringResource(id = R.string.add_item),
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
                    masterPasswordRepromptSubmit =
                    vaultItemListingHandlers.masterPasswordRepromptSubmit,
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
