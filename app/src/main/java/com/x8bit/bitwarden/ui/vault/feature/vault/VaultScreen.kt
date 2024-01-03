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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The vault screen for the application.
 */
@Suppress("LongMethod")
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onNavigateToVaultAddItemScreen: () -> Unit,
    onNavigateToVaultItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItemScreen: (vaultItemId: String) -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    intentHandler: IntentHandler = IntentHandler(LocalContext.current),
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

            is VaultEvent.NavigateToVaultItem -> onNavigateToVaultItemScreen(event.itemId)

            is VaultEvent.NavigateToEditVaultItem -> onNavigateToVaultEditItemScreen(event.itemId)

            is VaultEvent.NavigateToItemListing -> {
                onNavigateToVaultItemListingScreen(event.itemListingType)
            }

            VaultEvent.NavigateOutOfApp -> intentHandler.exitApplication()
            is VaultEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    VaultScreenScaffold(
        state = viewModel.stateFlow.collectAsState().value,
        vaultFilterTypeSelect = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.VaultFilterTypeSelect(it)) }
        },
        addItemClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.AddItemClick) }
        },
        searchIconClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.SearchIconClick) }
        },
        accountLockClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.LockAccountClick(it)) }
        },
        accountLogoutClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.LogoutAccountClick(it)) }
        },
        accountSwitchClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.SwitchAccountClick(it)) }
        },
        addAccountClickAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.AddAccountClick) }
        },
        syncAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.SyncClick) }
        },
        lockAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.LockClick) }
        },
        exitConfirmationAction = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.ExitConfirmationClick) }
        },
        onDimBottomNavBarRequest = onDimBottomNavBarRequest,
        vaultItemClick = remember(viewModel) {
            { vaultItem -> viewModel.trySendAction(VaultAction.VaultItemClick(vaultItem)) }
        },
        folderClick = remember(viewModel) {
            { folderItem -> viewModel.trySendAction(VaultAction.FolderClick(folderItem)) }
        },
        collectionClick = remember(viewModel) {
            { collectionItem ->
                viewModel.trySendAction(VaultAction.CollectionClick(collectionItem))
            }
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
        tryAgainClick = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.TryAgainClick) }
        },
        dialogDismiss = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.DialogDismiss) }
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
    vaultFilterTypeSelect: (VaultFilterType) -> Unit,
    addItemClickAction: () -> Unit,
    searchIconClickAction: () -> Unit,
    accountLockClickAction: (AccountSummary) -> Unit,
    accountLogoutClickAction: (AccountSummary) -> Unit,
    accountSwitchClickAction: (AccountSummary) -> Unit,
    addAccountClickAction: () -> Unit,
    syncAction: () -> Unit,
    lockAction: () -> Unit,
    exitConfirmationAction: () -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    vaultItemClick: (VaultState.ViewState.VaultItem) -> Unit,
    folderClick: (VaultState.ViewState.FolderItem) -> Unit,
    collectionClick: (VaultState.ViewState.CollectionItem) -> Unit,
    loginGroupClick: () -> Unit,
    cardGroupClick: () -> Unit,
    identityGroupClick: () -> Unit,
    secureNoteGroupClick: () -> Unit,
    trashClick: () -> Unit,
    tryAgainClick: () -> Unit,
    dialogDismiss: () -> Unit,
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
                onDismissRequest = dialogDismiss,
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
                exitConfirmationAction()
            },
            onDismissClick = { shouldShowExitConfirmationDialog = false },
            onDismissRequest = { shouldShowExitConfirmationDialog = false },
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
                    )
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = searchIconClickAction,
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.sync),
                                onClick = syncAction,
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.lock),
                                onClick = lockAction,
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
        Box {
            val innerModifier = Modifier
                .fillMaxSize()
            val outerModifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            Column(modifier = outerModifier) {
                val vaultFilterData = state.vaultFilterData
                if (state.viewState.hasVaultFilter && vaultFilterData != null) {
                    VaultFilter(
                        selectedVaultFilterType = vaultFilterData.selectedVaultFilterType,
                        vaultFilterTypes = vaultFilterData.vaultFilterTypes.toImmutableList(),
                        onVaultFilterTypeSelect = vaultFilterTypeSelect,
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

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }

                when (val viewState = state.viewState) {
                    is VaultState.ViewState.Content -> VaultContent(
                        state = viewState,
                        vaultItemClick = vaultItemClick,
                        folderClick = folderClick,
                        collectionClick = collectionClick,
                        loginGroupClick = loginGroupClick,
                        cardGroupClick = cardGroupClick,
                        identityGroupClick = identityGroupClick,
                        secureNoteGroupClick = secureNoteGroupClick,
                        trashClick = trashClick,
                        modifier = innerModifier,
                    )

                    is VaultState.ViewState.Loading -> BitwardenLoadingContent(
                        modifier = innerModifier,
                    )

                    is VaultState.ViewState.NoItems -> VaultNoItems(
                        modifier = innerModifier,
                        addItemClickAction = addItemClickAction,
                    )

                    is VaultState.ViewState.Error -> BitwardenErrorContent(
                        message = viewState.message(),
                        onTryAgainClick = tryAgainClick,
                        modifier = innerModifier,
                    )
                }
            }

            BitwardenAccountSwitcher(
                isVisible = accountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = accountSwitchClickAction,
                onLockAccountClick = accountLockClickAction,
                onLogoutAccountClick = accountLogoutClickAction,
                onAddAccountClick = addAccountClickAction,
                onDismissRequest = { updateAccountMenuVisibility(false) },
                topAppBarScrollBehavior = scrollBehavior,
                modifier = outerModifier,
            )
        }
    }
}
