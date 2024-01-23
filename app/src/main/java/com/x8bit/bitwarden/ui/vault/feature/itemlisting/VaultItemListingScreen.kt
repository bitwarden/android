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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers.VaultItemListingHandlers
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the vault item listing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemListingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItem: (id: String) -> Unit,
    onNavigateToVaultEditItemScreen: (cipherId: String) -> Unit,
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToAddSendItem: () -> Unit,
    onNavigateToEditSendItem: (sendId: String) -> Unit,
    onNavigateToSearch: (searchType: SearchType) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: VaultItemListingViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val resources = context.resources

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
                onNavigateToVaultAddItemScreen()
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
        }
    }

    VaultItemListingDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick) }
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.itemListingType.titleText(),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = vaultItemListingHandlers.backClick,
                actions = {
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = vaultItemListingHandlers.searchIconClick,
                    )
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
                },
            )
        },
        floatingActionButton = {
            if (state.itemListingType.hasFab) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = vaultItemListingHandlers.addVaultItemClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
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
                    vaultItemClick = vaultItemListingHandlers.itemClick,
                    onOverflowItemClick = vaultItemListingHandlers.overflowItemClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.NoItems -> {
                VaultItemListingEmpty(
                    itemListingType = state.itemListingType,
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
    }
}
