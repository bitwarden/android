package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar

/**
 * Displays the vault item listing screen.
 */
@Composable
fun VaultItemListingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItem: (id: String) -> Unit,
    onNavigateToVaultAddItemScreen: () -> Unit,
    viewModel: VaultItemListingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultItemListingEvent.NavigateBack -> onNavigateBack()

            is VaultItemListingEvent.NavigateToVaultItem -> {
                onNavigateToVaultItem(event.id)
            }

            is VaultItemListingEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultItemListingEvent.NavigateToAddVaultItem -> {
                onNavigateToVaultAddItemScreen()
            }

            is VaultItemListingEvent.NavigateToVaultSearchScreen -> {
                // TODO Create vault search screen and navigation implementation BIT-213
                Toast
                    .makeText(context, "Navigate to the vault search screen.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    VaultItemListingScaffold(
        state = viewModel.stateFlow.collectAsState().value,
        backClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.BackClick) }
        },
        searchIconClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.SearchIconClick) }
        },
        addVaultItemClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick) }
        },
        vaultItemClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.ItemClick(it)) }
        },
        refreshClick = remember(viewModel) {
            { viewModel.trySendAction(VaultItemListingsAction.RefreshClick) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun VaultItemListingScaffold(
    state: VaultItemListingState,
    backClick: () -> Unit,
    searchIconClick: () -> Unit,
    addVaultItemClick: () -> Unit,
    vaultItemClick: (id: String) -> Unit,
    refreshClick: () -> Unit,
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
                onNavigationIconClick = backClick,
                actions = {
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = searchIconClick,
                    )
                    BitwardenOverflowActionItem()
                },
            )
        },
        floatingActionButton = {
            if (state.itemListingType.hasFab) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = addVaultItemClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                    )
                }
            }
        },
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)

        when (state.viewState) {
            is VaultItemListingState.ViewState.Content -> {
                VaultItemListingContent(
                    state = state.viewState,
                    vaultItemClick = vaultItemClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.NoItems -> {
                VaultItemListingEmpty(
                    itemListingType = state.itemListingType,
                    addItemClickAction = addVaultItemClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.Error -> {
                VaultItemListingError(
                    state = state.viewState,
                    onRefreshClick = refreshClick,
                    modifier = modifier,
                )
            }

            is VaultItemListingState.ViewState.Loading -> {
                VaultItemListingLoading(modifier = modifier)
            }
        }
    }
}
