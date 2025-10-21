package com.x8bit.bitwarden.ui.vault.feature.vault

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.bitwarden.ui.platform.components.animation.AnimateNullableContentVisibility
import com.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.appbar.model.TopAppBarDividerStyle
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.BitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbar
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.composition.LocalAppReviewManager
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.vault.components.VaultItemSelectionDialog
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
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
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToAutofillScreen: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    appReviewManager: AppReviewManager = LocalAppReviewManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
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
            VaultEvent.NavigateToImportLogins -> onNavigateToImportLogins()
            is VaultEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            VaultEvent.PromptForAppReview -> {
                launchPrompt.invoke()
            }

            VaultEvent.NavigateToAddFolder -> {
                onNavigateToAddFolderScreen(null)
            }

            VaultEvent.NavigateToAbout -> onNavigateToAboutScreen()

            is VaultEvent.ShowShareSheet -> {
                intentManager.shareText(event.content)
            }

            VaultEvent.NavigateToAutofillSettings -> onNavigateToAutofillScreen()
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { !accountMenuVisible },
    )

    // Dynamic dialogs
    VaultDialogs(dialogState = state.dialog, vaultHandlers = vaultHandlers)
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
                        contentDescription = stringResource(id = BitwardenString.search_vault),
                        onClick = vaultHandlers.searchIconClickAction,
                    )
                    BitwardenOverflowActionItem(
                        contentDescription = stringResource(BitwardenString.more),
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = BitwardenString.sync),
                                onClick = vaultHandlers.syncAction,
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = BitwardenString.lock),
                                onClick = vaultHandlers.lockAction,
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
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_plus_large),
                    contentDescription = stringResource(id = BitwardenString.add_item),
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
                            cardTitle = stringResource(BitwardenString.import_saved_logins),
                            cardSubtitle = stringResource(
                                BitwardenString.use_a_computer_to_import_logins,
                            ),
                            actionText = stringResource(BitwardenString.get_started),
                            onActionClick = vaultHandlers.importActionCardClick,
                            onDismissClick = vaultHandlers.dismissImportActionCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .standardHorizontalMargin()
                                .padding(top = 12.dp),
                        )
                    }
                    VaultNoItems(
                        vectorRes = BitwardenDrawable.ill_vault_items,
                        headerText = stringResource(
                            id = BitwardenString.save_and_protect_your_data,
                        ),
                        message = stringResource(
                            BitwardenString.the_vault_protects_more_than_just_passwords,
                        ),
                        buttonText = stringResource(BitwardenString.new_login),
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

@Suppress("LongMethod")
@Composable
private fun VaultDialogs(
    dialogState: VaultState.DialogState?,
    vaultHandlers: VaultHandlers,
) {
    when (dialogState) {
        is VaultState.DialogState.Syncing -> BitwardenLoadingDialog(
            text = stringResource(id = BitwardenString.syncing),
        )

        is VaultState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title(),
            message = dialogState.message(),
            throwable = dialogState.error,
            onDismissRequest = vaultHandlers.dialogDismiss,
        )

        is VaultState.DialogState.SelectVaultAddItemType -> VaultItemSelectionDialog(
            onOptionSelected = {
                vaultHandlers.addItemClickAction(it)
            },
            onDismissRequest = vaultHandlers.dialogDismiss,
            excludedOptions = dialogState.excludedOptions,
        )

        is VaultState.DialogState.CipherDecryptionError -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                confirmButtonText = stringResource(BitwardenString.copy_error_report),
                dismissButtonText = stringResource(BitwardenString.close),
                onConfirmClick = {
                    vaultHandlers.onShareCipherDecryptionErrorClick(dialogState.selectedCipherId)
                },
                onDismissClick = vaultHandlers.dialogDismiss,
                onDismissRequest = vaultHandlers.dialogDismiss,
            )
        }

        is VaultState.DialogState.ThirdPartyBrowserAutofill -> {
            BitwardenTwoButtonDialog(
                title = stringResource(
                    id = BitwardenString.enable_browser_autofill_to_keep_filling_passwords,
                ),
                message = stringResource(
                    id = if (dialogState.browserCount > 1) {
                        BitwardenString.your_browser_recently_updated_how_autofill_works_plural
                    } else {
                        BitwardenString.your_browser_recently_updated_how_autofill_works_singular
                    },
                ),
                confirmButtonText = stringResource(id = BitwardenString.go_to_settings),
                dismissButtonText = stringResource(id = BitwardenString.not_now),
                onConfirmClick = vaultHandlers.onEnabledThirdPartyAutofillClick,
                onDismissClick = vaultHandlers.onDismissThirdPartyAutofillDialogClick,
                onDismissRequest = vaultHandlers.onDismissThirdPartyAutofillDialogClick,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        }

        is VaultState.DialogState.VaultLoadCipherDecryptionError -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = pluralStringResource(
                    id = BitwardenPlurals
                        .bitwarden_could_not_decrypt_x_vault_item_copy_and_share_description_long,
                    count = dialogState.cipherCount,
                    dialogState.cipherCount,
                ),
                confirmButtonText = stringResource(BitwardenString.copy_error_report),
                dismissButtonText = stringResource(BitwardenString.close),
                onConfirmClick = {
                    vaultHandlers.onShareAllCipherDecryptionErrorsClick()
                },
                onDismissClick = vaultHandlers.dialogDismiss,
                onDismissRequest = vaultHandlers.dialogDismiss,
            )
        }

        is VaultState.DialogState.VaultLoadKdfUpdateRequired -> {
            BitwardenMasterPasswordDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                dismissButtonText = stringResource(BitwardenString.later),
                onConfirmClick = {
                    vaultHandlers.onKdfUpdatePasswordRepromptSubmit(it)
                },
                onDismissRequest = vaultHandlers.dialogDismiss,
            )
        }

        null -> Unit
    }
}
