package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold

/**
 * Displays the item listing screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListingScreen(
    viewModel: ItemListingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAddItemScreen: () -> Unit,
    onNavigateToItemScreen: (id: String) -> Unit,
    onNavigateToEditItemScreen: (id: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ItemListingEvent.NavigateBack -> onNavigateBack()
            ItemListingEvent.DismissPullToRefresh -> pullToRefreshState.endRefresh()
            ItemListingEvent.NavigateToAddItem -> onNavigateToAddItemScreen()
            is ItemListingEvent.NavigateToItem -> onNavigateToItemScreen(event.id)
            is ItemListingEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_LONG)
                    .show()
            }

            is ItemListingEvent.NavigateToEditItem -> onNavigateToEditItemScreen(event.id)
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.bitwarden_authenticator),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(ItemListingAction.AddItemClick) }
                },
                modifier = Modifier
                    .semantics { testTag = "AddItemButton" }
                    .padding(bottom = 16.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = stringResource(id = R.string.add_item),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val currentState = state.viewState) {
                is ItemListingState.ViewState.Content -> {
                    LazyColumn {
                        items(currentState.itemList) {
                            VaultVerificationCodeItem(
                                startIcon = it.startIcon,
                                label = it.label,
                                supportingLabel = it.supportingLabel,
                                timeLeftSeconds = it.timeLeftSeconds,
                                periodSeconds = it.periodSeconds,
                                authCode = it.authCode,
                                onCopyClick = { /*TODO*/ },
                                onItemClick = {
                                    onNavigateToItemScreen(it.id)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
                }

                is ItemListingState.ViewState.Error -> {
                    Text(text = "Error! ${currentState.message}", modifier = Modifier.fillMaxSize())
                }

                ItemListingState.ViewState.Loading -> {
                    Text(text = "Loading")
                }

                ItemListingState.ViewState.NoItems -> {
                    Text(text = "Welcome! Add a 2FA TOTP", modifier = Modifier.fillMaxSize())
                }
            }

            when (val dialog = state.dialog) {
                ItemListingState.DialogState.Syncing -> {
                    BitwardenLoadingDialog(
                        visibilityState = LoadingDialogState.Shown(
                            text = R.string.syncing.asText(),
                        ),
                    )
                }

                is ItemListingState.DialogState.Error -> {
                    BitwardenBasicDialog(
                        visibilityState = BasicDialogState.Shown(
                            title = dialog.title,
                            message = dialog.message,
                        ),
                        onDismissRequest = {
                            viewModel.trySendAction(ItemListingAction.DialogDismiss)
                        },
                    )
                }

                null -> Unit
            }
        }
    }
}
