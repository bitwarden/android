package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalExitManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The vault screen for the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVerificationCodeScreen: () -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    exitManager: ExitManager = LocalExitManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState().takeIf { state.isPullToRefreshEnabled }
    LaunchedEffect(key1 = pullToRefreshState?.isRefreshing) {
        if (pullToRefreshState?.isRefreshing == true) {
            viewModel.trySendAction(VaultAction.RefreshPull)
        }
    }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultEvent.DismissPullToRefresh -> pullToRefreshState?.endRefresh()

            VaultEvent.NavigateToAddItemScreen -> onNavigateToVaultAddItemScreen()

            VaultEvent.NavigateToVaultSearchScreen -> onNavigateToSearchVault(SearchType.Vault.All)

            is VaultEvent.NavigateToVerificationCodeScreen -> {
                onNavigateToVerificationCodeScreen()
            }

            is VaultEvent.NavigateToVaultItem -> onNavigateToVaultItemScreen(event.itemId)

            is VaultEvent.NavigateToEditVaultItem -> onNavigateToVaultEditItemScreen(event.itemId)

            is VaultEvent.NavigateToItemListing -> {
                onNavigateToVaultItemListingScreen(event.itemListingType)
            }

            is VaultEvent.NavigateToUrl -> intentManager.launchUri(event.url.toUri())

            VaultEvent.NavigateOutOfApp -> exitManager.exitApplication()
            is VaultEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    val vaultHandlers = remember(viewModel) { VaultHandlers.create(viewModel) }
    VaultScreenScaffold(
        state = state,
        pullToRefreshState = pullToRefreshState,
        vaultHandlers = vaultHandlers,
        onDimBottomNavBarRequest = onDimBottomNavBarRequest,
    )
}

/**
 * Scaffold for the [VaultScreen]
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultScreenScaffold(
    state: VaultState,
    pullToRefreshState: PullToRefreshState?,
    vaultHandlers: VaultHandlers,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
) {
    var accountMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val updateAccountMenuVisibility = { shouldShowMenu: Boolean ->
        accountMenuVisible = shouldShowMenu
        onDimBottomNavBarRequest(shouldShowMenu)
    }
    var shouldShowExitConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { !accountMenuVisible },
        )

    // Dynamic dialogs
    when (val dialog = state.dialog) {
        is VaultState.DialogState.Syncing -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = R.string.syncing.asText(),
                ),
            )
        }

        is VaultState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title,
                    message = dialog.message,
                ),
                onDismissRequest = vaultHandlers.dialogDismiss,
            )
        }

        null -> Unit
    }

    // Static dialogs
    if (shouldShowExitConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.exit),
            message = stringResource(id = R.string.exit_confirmation),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                shouldShowExitConfirmationDialog = false
                vaultHandlers.exitConfirmationAction()
            },
            onDismissClick = { shouldShowExitConfirmationDialog = false },
            onDismissRequest = { shouldShowExitConfirmationDialog = false },
        )
    }

    var masterPasswordRepromptAction by remember {
        mutableStateOf<ListingItemOverflowAction.VaultAction?>(null)
    }
    masterPasswordRepromptAction?.let { action ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                masterPasswordRepromptAction = null
                vaultHandlers.masterPasswordRepromptSubmit(action, password)
            },
            onDismissRequest = {
                masterPasswordRepromptAction = null
            },
        )
    }

    BitwardenScaffold(
        topBar = {
            BitwardenMediumTopAppBar(
                title = state.appBarTitle(),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenAccountActionItem(
                        initials = state.initials,
                        color = state.avatarColor,
                        onClick = {
                            updateAccountMenuVisibility(!accountMenuVisible)
                        },
                        actionTestTag = "AccountIconButton",
                    )
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = vaultHandlers.searchIconClickAction,
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.sync),
                                onClick = vaultHandlers.syncAction,
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.lock),
                                onClick = vaultHandlers.lockAction,
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.exit),
                                onClick = { shouldShowExitConfirmationDialog = true },
                            ),
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.viewState.hasFab && !accountMenuVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = vaultHandlers.addItemClickAction,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        pullToRefreshState = pullToRefreshState,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Box {
            val innerModifier = Modifier
                .fillMaxSize()
            val outerModifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            Column(modifier = outerModifier) {
                state.vaultFilterDataWithFilter?.let {
                    VaultFilter(
                        selectedVaultFilterType = it.selectedVaultFilterType,
                        vaultFilterTypes = it.vaultFilterTypes.toImmutableList(),
                        onVaultFilterTypeSelect = vaultHandlers.vaultFilterTypeSelect,
                        topAppBarScrollBehavior = scrollBehavior,
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                // There is some built-in padding to the menu button that makes up
                                // the visual difference here.
                                end = 12.dp,
                            )
                            .fillMaxWidth(),
                    )
                }

                when (val viewState = state.viewState) {
                    is VaultState.ViewState.Content -> VaultContent(
                        state = viewState,
                        vaultHandlers = vaultHandlers,
                        onOverflowOptionClick = { masterPasswordRepromptAction = it },
                        modifier = innerModifier,
                    )

                    is VaultState.ViewState.Loading -> BitwardenLoadingContent(
                        modifier = innerModifier,
                    )

                    is VaultState.ViewState.NoItems -> VaultNoItems(
                        modifier = innerModifier,
                        policyDisablesSend = false,
                        addItemClickAction = vaultHandlers.addItemClickAction,
                    )

                    is VaultState.ViewState.Error -> BitwardenErrorContent(
                        message = viewState.message(),
                        onTryAgainClick = vaultHandlers.tryAgainClick,
                        modifier = innerModifier,
                    )
                }
            }

            BitwardenAccountSwitcher(
                isVisible = accountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = vaultHandlers.accountSwitchClickAction,
                onLockAccountClick = vaultHandlers.accountLockClickAction,
                onLogoutAccountClick = vaultHandlers.accountLogoutClickAction,
                onAddAccountClick = vaultHandlers.addAccountClickAction,
                onDismissRequest = { updateAccountMenuVisibility(false) },
                topAppBarScrollBehavior = scrollBehavior,
                modifier = outerModifier,
            )
        }
    }
}
