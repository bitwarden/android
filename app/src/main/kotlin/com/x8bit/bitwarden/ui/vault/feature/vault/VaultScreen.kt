package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.animation.AnimateNullableContentVisibility
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.model.BitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.model.TopAppBarDividerStyle
import com.x8bit.bitwarden.ui.platform.components.model.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbar
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.components.snackbar.rememberBitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.composition.LocalAppReviewManager
import com.x8bit.bitwarden.ui.platform.composition.LocalExitManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.components.VaultItemSelectionDialog
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_REVIEW_DELAY = 3000L

/**
 * The vault screen for the application.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVerificationCodeScreen: () -> Unit,
    onNavigateToVaultItemListingScreen: (vaultItemType: VaultItemListingType) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    exitManager: ExitManager = LocalExitManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    appReviewManager: AppReviewManager = LocalAppReviewManager.current,
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
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.trySendAction(VaultAction.LifecycleResumed)
            }

            else -> Unit
        }
    }
    val scope = rememberCoroutineScope()
    val launchPrompt = remember {
        {
            scope.launch {
                delay(APP_REVIEW_DELAY)
                appReviewManager.promptForReview()
            }
        }
    }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultEvent.NavigateToAddItemScreen -> {
                onNavigateToVaultAddItemScreen(
                    VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.AddItem,
                        vaultItemCipherType = event.type,
                    ),
                )
            }

            VaultEvent.NavigateToVaultSearchScreen -> onNavigateToSearchVault(SearchType.Vault.All)

            is VaultEvent.NavigateToVerificationCodeScreen -> {
                onNavigateToVerificationCodeScreen()
            }

            is VaultEvent.NavigateToVaultItem -> {
                onNavigateToVaultItemScreen(VaultItemArgs(event.itemId, event.type))
            }

            is VaultEvent.NavigateToEditVaultItem -> {
                onNavigateToVaultEditItemScreen(
                    VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = event.itemId),
                        vaultItemCipherType = event.type,
                    ),
                )
            }

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

            VaultEvent.NavigateToImportLogins -> {
                onNavigateToImportLogins(SnackbarRelay.MY_VAULT_RELAY)
            }

            is VaultEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            VaultEvent.PromptForAppReview -> {
                launchPrompt.invoke()
            }

            VaultEvent.NavigateToAddFolder -> {
                onNavigateToAddFolderScreen(null)
            }

            VaultEvent.NavigateToAbout -> onNavigateToAboutScreen()
        }
    }
    val vaultHandlers = remember(viewModel) { VaultHandlers.create(viewModel) }
    VaultScreenScaffold(
        state = state,
        pullToRefreshState = pullToRefreshState,
        vaultHandlers = vaultHandlers,
        onDimBottomNavBarRequest = onDimBottomNavBarRequest,
        snackbarHostState = snackbarHostState,
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
    pullToRefreshState: BitwardenPullToRefreshState,
    vaultHandlers: VaultHandlers,
    onDimBottomNavBarRequest: (shouldDim: Boolean) -> Unit,
    snackbarHostState: BitwardenSnackbarHostState,
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
        utilityBar = {
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
        },
        snackbarHost = {
            AnimateNullableContentVisibility(
                targetState = state.flightRecorderSnackBar,
                label = "AnimateFlightRecorderSnackbar",
            ) { data ->
                BitwardenSnackbar(
                    bitwardenSnackbarData = data,
                    onDismiss = vaultHandlers.dismissFlightRecorderSnackbar,
                    onActionClick = vaultHandlers.flightRecorderGoToSettingsClick,
                )
            }
            if (state.flightRecorderSnackBar == null) {
                // We don't want additional animations from the Animated Visibility and we only
                // want this displayed if the flight recorder snackbar is not set.
                BitwardenSnackbarHost(
                    bitwardenHostState = snackbarHostState,
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.viewState.hasFab && !accountMenuVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                BitwardenFloatingActionButton(
                    onClick = vaultHandlers.selectAddItemTypeClickAction,
                    painter = rememberVectorPainter(id = R.drawable.ic_plus_large),
                    contentDescription = stringResource(id = R.string.add_item),
                    modifier = Modifier.testTag(tag = "AddItemButton"),
                )
            }
        },
        overlay = {
            BitwardenAccountSwitcher(
                isVisible = accountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = vaultHandlers.accountSwitchClickAction,
                onLockAccountClick = vaultHandlers.accountLockClickAction,
                onLogoutAccountClick = vaultHandlers.accountLogoutClickAction,
                onAddAccountClick = vaultHandlers.addAccountClickAction,
                onDismissRequest = { updateAccountMenuVisibility(false) },
                topAppBarScrollBehavior = scrollBehavior,
                modifier = Modifier.fillMaxSize(),
            )
        },
        pullToRefreshState = pullToRefreshState,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val viewState = state.viewState) {
                is VaultState.ViewState.Content -> VaultContent(
                    state = viewState,
                    vaultHandlers = vaultHandlers,
                    onOverflowOptionClick = { masterPasswordRepromptAction = it },
                    modifier = Modifier.fillMaxSize(),
                )

                is VaultState.ViewState.Loading -> BitwardenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
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
                        vectorRes = R.drawable.img_vault_items,
                        headerText = stringResource(id = R.string.save_and_protect_your_data),
                        message = stringResource(
                            R.string.the_vault_protects_more_than_just_passwords,
                        ),
                        buttonText = stringResource(R.string.new_login),
                        policyDisablesSend = false,
                        addItemClickAction = {
                            vaultHandlers.addItemClickAction(CreateVaultItemType.LOGIN)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is VaultState.ViewState.Error -> BitwardenErrorContent(
                    message = viewState.message(),
                    onTryAgainClick = vaultHandlers.tryAgainClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
            text = stringResource(id = R.string.syncing),
        )

        is VaultState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title(),
            message = dialogState.message(),
            throwable = dialogState.error,
            onDismissRequest = vaultHandlers.dialogDismiss,
        )

        VaultState.DialogState.SelectVaultAddItemType -> VaultItemSelectionDialog(
            onOptionSelected = {
                vaultHandlers.addItemClickAction(it)
            },
            onDismissRequest = vaultHandlers.dialogDismiss,
        )

        null -> Unit
    }
}
