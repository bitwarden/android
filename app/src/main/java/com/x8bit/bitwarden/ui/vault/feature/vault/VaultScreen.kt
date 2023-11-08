package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem

/**
 * The vault screen for the application.
 */
@Suppress("LongMethod")
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onNavigateToVaultAddItemScreen: () -> Unit,
) {
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultEvent.NavigateToAddItemScreen -> onNavigateToVaultAddItemScreen()

            VaultEvent.NavigateToVaultSearchScreen -> {
                // TODO Create vault search screen and navigation implementation BIT-213
                Toast
                    .makeText(context, "Navigate to the vault search screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            is VaultEvent.NavigateToVaultItem -> {
                Toast
                    .makeText(context, "Navigate to the item details screen", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToCardGroup -> {
                Toast
                    .makeText(context, "Navigate to card type screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            is VaultEvent.NavigateToFolder -> {
                Toast
                    .makeText(context, "Navigate to folder screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToIdentityGroup -> {
                Toast
                    .makeText(context, "Navigate to identity type screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToLoginGroup -> {
                Toast
                    .makeText(context, "Navigate to login type screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToSecureNotesGroup -> {
                Toast
                    .makeText(context, "Navigate to secure notes type screen.", Toast.LENGTH_SHORT)
                    .show()
            }

            VaultEvent.NavigateToTrash -> {
                Toast
                    .makeText(context, "Navigate to trash screen.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    VaultScreenScaffold(
        state = viewModel.stateFlow.collectAsState().value,
        addItemClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.AddItemClick) }
        },
        searchIconClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.SearchIconClick) }
        },
        vaultItemClick = remember(viewModel) {
            { vaultItem -> viewModel.trySendAction(VaultAction.VaultItemClick(vaultItem)) }
        },
        folderClick = remember(viewModel) {
            { folderItem -> viewModel.trySendAction(VaultAction.FolderClick(folderItem)) }
        },
        loginGroupClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.LoginGroupClick) }
        },
        cardGroupClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.CardGroupClick) }
        },
        identityGroupClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.IdentityGroupClick) }
        },
        secureNoteGroupClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.SecureNoteGroupClick) }
        },
        trashClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.TrashClick) }
        },
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
    vaultItemClick: (VaultState.ViewState.VaultItem) -> Unit,
    folderClick: (VaultState.ViewState.FolderItem) -> Unit,
    loginGroupClick: () -> Unit,
    cardGroupClick: () -> Unit,
    identityGroupClick: () -> Unit,
    secureNoteGroupClick: () -> Unit,
    trashClick: () -> Unit,
) {
    // TODO Create account menu and logging in ability BIT-205
    var accountMenuVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    BitwardenScaffold(
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
        when (val viewState = state.viewState) {
            is VaultState.ViewState.Content -> VaultContent(
                state = viewState,
                vaultItemClick = vaultItemClick,
                folderClick = folderClick,
                loginGroupClick = loginGroupClick,
                cardGroupClick = cardGroupClick,
                identityGroupClick = identityGroupClick,
                secureNoteGroupClick = secureNoteGroupClick,
                trashClick = trashClick,
                paddingValues = paddingValues,
            )

            is VaultState.ViewState.Loading -> VaultLoading(paddingValues = paddingValues)
            is VaultState.ViewState.NoItems -> VaultNoItems(
                paddingValues = paddingValues,
                addItemClickAction = addItemClickAction,
            )
        }
    }
}
