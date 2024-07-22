package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenOverwritePasskeyConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalFido2CompletionManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun VaultItemListingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItem: (id: String) -> Unit,
    onNavigateToVaultEditItemScreen: (cipherVaultId: String) -> Unit,
    onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    onNavigateToVaultAddItemScreen: (vaultItemCipherType: VaultItemCipherType) -> Unit,
    onNavigateToAddSendItem: () -> Unit,
    onNavigateToEditSendItem: (sendId: String) -> Unit,
    onNavigateToSearch: (searchType: SearchType) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
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

    val pullToRefreshState = rememberPullToRefreshState().takeIf { state.isPullToRefreshEnabled }
    LaunchedEffect(key1 = pullToRefreshState?.isRefreshing) {
        if (pullToRefreshState?.isRefreshing == true) {
            viewModel.trySendAction(VaultItemListingsAction.RefreshPull)
        }
    }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultItemListingEvent.NavigateBack -> onNavigateBack()

            is VaultItemListingEvent.DismissPullToRefresh -> pullToRefreshState?.endRefresh()

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
                onNavigateToVaultAddItemScreen(event.vaultItemCipherType)
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
                    VaultItemListingsAction.DismissFido2CreationErrorDialogClick,
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
        onDismissFido2PasswordVerification = remember(viewModel) {
            {
                viewModel.trySendAction(
                    VaultItemListingsAction.DismissFido2PasswordVerificationDialogClick,
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
    )

    VaultItemListingScaffold(
        state = state,
        pullToRefreshState = pullToRefreshState,
        vaultItemListingHandlers = remember(viewModel) {
            VaultItemListingHandlers.create(viewModel)
        },
    )
}

@Composable
private fun VaultItemListingDialogs(
    dialogState: VaultItemListingState.DialogState?,
    onDismissRequest: () -> Unit,
    onDismissFido2ErrorDialog: () -> Unit,
    onConfirmOverwriteExistingPasskey: (cipherViewId: String) -> Unit,
    onSubmitMasterPasswordFido2Verification: (password: String, cipherId: String) -> Unit,
    onDismissFido2PasswordVerification: () -> Unit,
    onRetryFido2PasswordVerification: (cipherViewId: String) -> Unit,
) {
    when (dialogState) {
        is VaultItemListingState.DialogState.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is VaultItemListingState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(dialogState.message),
        )

        is VaultItemListingState.DialogState.Fido2CreationFail -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
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
                onDismissRequest = onDismissFido2PasswordVerification,
            )
        }

        is VaultItemListingState.DialogState.Fido2MasterPasswordError -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = {
                    onRetryFido2PasswordVerification(dialogState.selectedCipherId)
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
    pullToRefreshState: PullToRefreshState?,
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
            if (state.itemListingType.hasFab) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = vaultItemListingHandlers.addVaultItemClick,
                    modifier = Modifier.testTag("AddItemButton"),
                ) {
                    Icon(
                        painter = rememberVectorPainter(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                    )
                }
            }
        },
        pullToRefreshState = pullToRefreshState,
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)

        when (state.viewState) {
            is VaultItemListingState.ViewState.Content -> {
                VaultItemListingContent(
                    state = state.viewState,
                    policyDisablesSend = state.policyDisablesSend &&
                        state.itemListingType is VaultItemListingState.ItemListingType.Send,
                    vaultItemClick = vaultItemListingHandlers.itemClick,
                    collectionClick = vaultItemListingHandlers.collectionClick,
                    folderClick = vaultItemListingHandlers.folderClick,
                    masterPasswordRepromptSubmit =
                    vaultItemListingHandlers.masterPasswordRepromptSubmit,
                    onOverflowItemClick = vaultItemListingHandlers.overflowItemClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.NoItems -> {
                VaultItemListingEmpty(
                    state = state.viewState,
                    policyDisablesSend = state.policyDisablesSend &&
                        state.itemListingType is VaultItemListingState.ItemListingType.Send,
                    addItemClickAction = vaultItemListingHandlers.addVaultItemClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = state.viewState.message(),
                    onTryAgainClick = vaultItemListingHandlers.refreshClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = modifier)
            }
        }

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
            modifier = modifier,
        )
    }
}
