package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect

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
    Scaffold(
        topBar = {
            VaultTopBar(
                accountIconClickAction = { accountMenuVisible = !accountMenuVisible },
                searchIconClickAction = searchIconClickAction,
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = addItemClickAction,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add_item),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { paddingValues ->
        when (state) {
            is VaultState.Content -> VaultContentView(paddingValues = paddingValues)
            is VaultState.Loading -> VaultLoadingView(paddingValues = paddingValues)
            is VaultState.NoItems -> VaultNoItemsView(
                paddingValues = paddingValues,
                addItemClickAction = addItemClickAction,
            )
        }
    }
}
