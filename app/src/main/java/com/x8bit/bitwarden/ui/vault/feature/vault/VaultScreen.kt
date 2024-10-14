package com.x8bit.bitwarden.ui.vault.feature.vault

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.x8bit.bitwarden.ui.platform.components.model.TopAppBarDividerStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.scaffold.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalExitManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
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
    onNavigateToVerificationCodeScreen: () -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: () -> Unit,
    exitManager: ExitManager = LocalExitManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = remember(viewModel) {
            { viewModel.trySendAction(VaultAction.RefreshPull) }
        },
    )
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
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

            VaultEvent.NavigateToImportLogins -> onNavigateToImportLogins()
        }
    }
    val vaultHandlers = remember(viewModel) { VaultHandlers.create(viewModel) }
    VaultScreenPushNotifications(
        hideNotificationsDialog = state.hideNotificationsDialog,
        permissionsManager = permissionsManager,
    )
    VaultScreenScaffold(
        state = state,
        pullToRefreshState = pullToRefreshState,
        vaultHandlers = vaultHandlers,
        onDimBottomNavBarRequest = onDimBottomNavBarRequest,
    )
}

/**
 * Handles the notifications permission request.
 */
@Composable
private fun VaultScreenPushNotifications(
    hideNotificationsDialog: Boolean,
    permissionsManager: PermissionsManager,
) {
    if (hideNotificationsDialog) return
    val launcher = permissionsManager.getLauncher {
        // We do not actually care what the response is, we just need
        // to give the user a chance to give us the permission.
    }
    LaunchedEffect(key1 = Unit) {
        @SuppressLint("InlinedApi")
        // We check the version code as part of the 'hideNotificationsDialog' property.
        if (!permissionsManager.checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Scaffold for the [VaultScreen]
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultScreenScaffold(
    state: VaultState,
    pullToRefreshState: BitwardenPullToRefreshState,
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { !accountMenuVisible },
    )

    // Dynamic dialogs
    VaultDialogs(dialogState = state.dialog, vaultHandlers = vaultHandlers)

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
                dividerStyle = state
                    .vaultFilterDataWithFilter
                    ?.let { TopAppBarDividerStyle.STATIC }
                    ?: TopAppBarDividerStyle.ON_SCROLL,
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
                BitwardenFloatingActionButton(
                    onClick = vaultHandlers.addItemClickAction,
                    painter = rememberVectorPainter(id = R.drawable.ic_plus_large),
                    contentDescription = stringResource(id = R.string.add_item),
                    modifier = Modifier.testTag(tag = "AddItemButton"),
                )
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

                    is VaultState.ViewState.NoItems -> {
                        AnimatedVisibility(
                            visible = state.showImportActionCard,
                            exit = actionCardExitAnimation(),
                            label = "VaultNoItemsActionCard",
                        ) {
                            BitwardenActionCard(
                                cardTitle = stringResource(R.string.import_saved_logins),
                                cardSubtitle = stringResource(
                                    R.string.use_a_computer_to_import_logins,
                                ),
                                actionText = stringResource(R.string.get_started),
                                onActionClick = vaultHandlers.importActionCardClick,
                                onDismissClick = vaultHandlers.dismissImportActionCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .standardHorizontalMargin()
                                    .padding(top = 12.dp),
                            )
                        }
                        VaultNoItems(
                            modifier = innerModifier,
                            policyDisablesSend = false,
                            addItemClickAction = vaultHandlers.addItemClickAction,
                        )
                    }

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

@Composable
private fun VaultDialogs(
    dialogState: VaultState.DialogState?,
    vaultHandlers: VaultHandlers,
) {
    when (dialogState) {
        is VaultState.DialogState.Syncing -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(
                text = R.string.syncing.asText(),
            ),
        )

        is VaultState.DialogState.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = vaultHandlers.dialogDismiss,
        )

        null -> Unit
    }
}
