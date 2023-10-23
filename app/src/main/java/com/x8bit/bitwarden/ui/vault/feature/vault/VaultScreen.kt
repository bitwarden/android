package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem

/**
 * The vault screen for the application.
 */
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultEvent.NavigateToAddItemScreen -> {
                // TODO Create add item screen and navigation implementation BIT-207
                Toast.makeText(context, "Navigate to the add item screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToVaultSearchScreen -> {
                // TODO Create vault search screen and navigation implementation BIT-213
                Toast.makeText(context, "Navigate to the vault search screen.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    VaultScreenScaffold(
        state = viewModel.stateFlow.collectAsState().value,
        addItemClickAction = { viewModel.trySendAction(VaultAction.AddItemClick) },
        searchIconClickAction = { viewModel.trySendAction(VaultAction.SearchIconClick) },
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
    addItemClickAction: () -> Unit,
    searchIconClickAction: () -> Unit,
) {
    // TODO Create account menu and logging in ability BIT-205
    var accountMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.my_vault),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenAccountActionItem(
                        initials = state.initials,
                        color = state.avatarColor,
                        onClick = { accountMenuVisible = !accountMenuVisible },
                    )
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = searchIconClickAction,
                    )
                    BitwardenOverflowActionItem()
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !accountMenuVisible,
                // The enter transition is required for AnimatedVisibility to work correctly on
                // FloatingActionButton. See - https://issuetracker.google.com/issues/224005027?pli=1
                enter = fadeIn() + expandIn { IntSize(width = 1, height = 1) },
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = addItemClickAction,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        when (state.viewState) {
            is VaultState.ViewState.Content -> VaultContentView(paddingValues = paddingValues)
            is VaultState.ViewState.Loading -> VaultLoadingView(paddingValues = paddingValues)
            is VaultState.ViewState.NoItems -> VaultNoItemsView(
                paddingValues = paddingValues,
                addItemClickAction = addItemClickAction,
            )
        }
    }
}
