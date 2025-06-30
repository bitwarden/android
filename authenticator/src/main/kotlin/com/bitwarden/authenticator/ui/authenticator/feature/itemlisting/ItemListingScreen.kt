package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.ItemListingExpandableFabAction
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorMediumTopAppBar
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorTopAppBar
import com.bitwarden.authenticator.ui.platform.components.appbar.action.AuthenticatorSearchActionItem
import com.bitwarden.authenticator.ui.platform.components.button.AuthenticatorFilledButton
import com.bitwarden.authenticator.ui.platform.components.button.AuthenticatorFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.button.AuthenticatorTextButton
import com.bitwarden.authenticator.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.fab.ExpandableFabIcon
import com.bitwarden.authenticator.ui.platform.components.fab.ExpandableFloatingActionButton
import com.bitwarden.authenticator.ui.platform.components.header.AuthenticatorExpandingHeader
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderTextWithSupportLabel
import com.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.composition.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.composition.LocalPermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.theme.Typography
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import kotlinx.coroutines.launch

/**
 * Displays the item listing screen.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListingScreen(
    viewModel: ItemListingViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItemScreen: (id: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val launcher = permissionsManager.getLauncher { isGranted ->
        if (isGranted) {
            viewModel.trySendAction(ItemListingAction.ScanQrCodeClick)
        } else {
            viewModel.trySendAction(ItemListingAction.EnterSetupKeyClick)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ItemListingEvent.NavigateBack -> onNavigateBack()
            is ItemListingEvent.NavigateToSearch -> onNavigateToSearch()
            is ItemListingEvent.NavigateToQrCodeScanner -> onNavigateToQrCodeScanner()
            is ItemListingEvent.NavigateToManualAddItem -> onNavigateToManualKeyEntry()
            is ItemListingEvent.ShowToast -> {
                Toast
                    .makeText(
                        context,
                        event.message(context.resources),
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }

            is ItemListingEvent.NavigateToEditItem -> onNavigateToEditItemScreen(event.id)
            is ItemListingEvent.NavigateToAppSettings -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:" + context.packageName)

                intentManager.startActivity(intent = intent)
            }

            ItemListingEvent.NavigateToBitwardenListing -> {
                intentManager.launchUri(
                    "https://play.google.com/store/apps/details?id=com.x8bit.bitwarden".toUri(),
                )
            }

            ItemListingEvent.NavigateToSyncInformation -> {
                intentManager.launchUri("https://bitwarden.com/help/totp-sync".toUri())
            }

            ItemListingEvent.NavigateToBitwardenSettings -> {
                intentManager.startMainBitwardenAppAccountSettings()
            }

            is ItemListingEvent.ShowFirstTimeSyncSnackbar -> {
                // Message property is overridden by FirstTimeSyncSnackbarHost:
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("")
                }
            }
        }
    }

    ItemListingDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(viewModel) {
            {
                viewModel.trySendAction(
                    ItemListingAction.DialogDismiss,
                )
            }
        },
        onConfirmDeleteClick = remember(viewModel) {
            { itemId ->
                viewModel.trySendAction(
                    ItemListingAction.ConfirmDeleteClick(itemId = itemId),
                )
            }
        },
    )

    when (val currentState = state.viewState) {
        is ItemListingState.ViewState.Content -> {
            ItemListingContent(
                state = currentState,
                snackbarHostState = snackbarHostState,
                scrollBehavior = scrollBehavior,
                onNavigateToSearch = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            ItemListingAction.SearchClick,
                        )
                    }
                },
                onScanQrCodeClick = remember(viewModel) {
                    {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                },
                onEnterSetupKeyClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.EnterSetupKeyClick)
                    }
                },
                onItemClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            ItemListingAction.ItemClick(it),
                        )
                    }
                },
                onDropdownMenuClick = remember(viewModel) {
                    { action, item ->
                        viewModel.trySendAction(
                            ItemListingAction.DropdownMenuClick(
                                menuAction = action,
                                item = item,
                            ),
                        )
                    }
                },
                onDownloadBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick)
                    }
                },
                onDismissDownloadBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss)
                    }
                },
                onSyncWithBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick)
                    }
                },
                onDismissSyncWithBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss)
                    }
                },
                onSyncLearnMoreClick = remember(viewModel) {
                    { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
                },
                onSectionExpandedClick = remember(viewModel) {
                    { viewModel.trySendAction(ItemListingAction.SectionExpandedClick(it)) }
                },
            )
        }

        ItemListingState.ViewState.Loading -> Unit
        is ItemListingState.ViewState.NoItems,
            -> {
            EmptyItemListingContent(
                actionCardState = currentState.actionCard,
                appTheme = state.appTheme,
                scrollBehavior = scrollBehavior,
                onAddCodeClick = remember(viewModel) {
                    {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                },
                onScanQrCodeClick = remember(viewModel) {
                    {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                },
                onEnterSetupKeyClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.EnterSetupKeyClick)
                    }
                },
                onDownloadBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick)
                    }
                },
                onDismissDownloadBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss)
                    }
                },
                onSyncWithBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick)
                    }
                },
                onSyncLearnMoreClick = remember(viewModel) {
                    { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
                },
                onDismissSyncWithBitwardenClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss)
                    }
                },
            )
        }
    }
}

@Composable
private fun ItemListingDialogs(
    dialog: ItemListingState.DialogState?,
    onDismissRequest: () -> Unit,
    onConfirmDeleteClick: (itemId: String) -> Unit,
) {
    when (dialog) {
        ItemListingState.DialogState.Loading -> {
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
                onDismissRequest = onDismissRequest,
            )
        }

        is ItemListingState.DialogState.DeleteConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.delete),
                message = dialog.message(),
                confirmButtonText = stringResource(id = R.string.okay),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = {
                    onConfirmDeleteClick(dialog.itemId)
                },
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemListingContent(
    state: ItemListingState.ViewState.Content,
    snackbarHostState: SnackbarHostState,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateToSearch: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onEnterSetupKeyClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onDropdownMenuClick: (VaultDropdownMenuAction, VerificationCodeDisplayItem) -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    onSectionExpandedClick: (SharedCodesDisplayState.SharedCodesAccountSection) -> Unit,
) {
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AuthenticatorMediumTopAppBar(
                title = stringResource(id = R.string.verification_codes),
                scrollBehavior = scrollBehavior,
                actions = {
                    AuthenticatorSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_codes),
                        onClick = onNavigateToSearch,
                    )
                },
            )
        },
        floatingActionButton = {
            ExpandableFloatingActionButton(
                modifier = Modifier
                    .semantics { testTag = "AddItemButton" }
                    .padding(bottom = 16.dp),
                label = R.string.add_item.asText(),
                items = listOf(
                    ItemListingExpandableFabAction.ScanQrCode(
                        label = R.string.scan_a_qr_code.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = BitwardenDrawable.ic_camera),
                            contentDescription = stringResource(id = R.string.scan_a_qr_code),
                            testTag = "ScanQRCodeButton",
                        ),
                        onScanQrCodeClick = onScanQrCodeClick,
                    ),
                    ItemListingExpandableFabAction.EnterSetupKey(
                        label = R.string.enter_key_manually.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = BitwardenDrawable.ic_keyboard),
                            contentDescription = stringResource(id = R.string.enter_key_manually),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onEnterSetupKeyClick = onEnterSetupKeyClick,
                    ),
                ),
                expandableFabIcon = ExpandableFabIcon(
                    iconData = IconResource(
                        iconPainter = painterResource(id = BitwardenDrawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        testTag = "AddItemButton",
                    ),
                    iconRotation = 45f,
                ),
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
        snackbarHost = { FirstTimeSyncSnackbarHost(state = snackbarHostState) },
    ) { paddingValues ->
        var isLocalHeaderExpanded by rememberSaveable { mutableStateOf(true) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item(key = "action_card") {
                ActionCard(
                    actionCardState = state.actionCard,
                    onDownloadBitwardenClick = onDownloadBitwardenClick,
                    onDownloadBitwardenDismissClick = onDismissDownloadBitwardenClick,
                    onSyncWithBitwardenClick = onSyncWithBitwardenClick,
                    onSyncWithBitwardenDismissClick = onDismissSyncWithBitwardenClick,
                    onSyncLearnMoreClick = onSyncLearnMoreClick,
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .animateItem(),
                )
            }
            if (state.favoriteItems.isNotEmpty()) {
                item(key = "favorites_header") {
                    BitwardenListHeaderTextWithSupportLabel(
                        label = stringResource(id = R.string.favorites),
                        supportingLabel = state.favoriteItems.count().toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(
                    items = state.favoriteItems,
                    key = { "favorite_item_${it.id}" },
                ) {
                    VaultVerificationCodeItem(
                        authCode = it.authCode,
                        primaryLabel = it.title,
                        secondaryLabel = it.subtitle,
                        periodSeconds = it.periodSeconds,
                        timeLeftSeconds = it.timeLeftSeconds,
                        alertThresholdSeconds = it.alertThresholdSeconds,
                        startIcon = it.startIcon,
                        onItemClick = { onItemClick(it.authCode) },
                        onDropdownMenuClick = { action ->
                            onDropdownMenuClick(action, it)
                        },
                        showMoveToBitwarden = it.showMoveToBitwarden,
                        allowLongPress = it.allowLongPressActions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "favorites_divider") {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                            .animateItem(),
                    )
                }
            }

            if (state.shouldShowLocalHeader) {
                item(key = "local_items_header") {
                    AuthenticatorExpandingHeader(
                        label = stringResource(id = R.string.local_codes, state.itemList.size),
                        isExpanded = isLocalHeaderExpanded,
                        onClick = { isLocalHeaderExpanded = !isLocalHeaderExpanded },
                        onClickLabel = if (isLocalHeaderExpanded) {
                            stringResource(R.string.local_items_are_expanded_click_to_collapse)
                        } else {
                            stringResource(R.string.local_items_are_collapsed_click_to_expand)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }

            if (isLocalHeaderExpanded) {
                items(
                    items = state.itemList,
                    key = { "local_item_${it.id}" },
                ) {
                    VaultVerificationCodeItem(
                        authCode = it.authCode,
                        primaryLabel = it.title,
                        secondaryLabel = it.subtitle,
                        periodSeconds = it.periodSeconds,
                        timeLeftSeconds = it.timeLeftSeconds,
                        alertThresholdSeconds = it.alertThresholdSeconds,
                        startIcon = it.startIcon,
                        onItemClick = { onItemClick(it.authCode) },
                        onDropdownMenuClick = { action ->
                            onDropdownMenuClick(action, it)
                        },
                        showMoveToBitwarden = it.showMoveToBitwarden,
                        allowLongPress = it.allowLongPressActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }
            }

            when (state.sharedItems) {
                is SharedCodesDisplayState.Codes -> {
                    state.sharedItems.sections.forEachIndexed { index, section ->
                        item(key = "sharedSection_${section.label}") {
                            AuthenticatorExpandingHeader(
                                label = section.label(),
                                isExpanded = section.isExpanded,
                                onClick = {
                                    onSectionExpandedClick(section)
                                },
                                onClickLabel = if (section.isExpanded) {
                                    stringResource(R.string.items_expanded_click_to_collapse)
                                } else {
                                    stringResource(R.string.items_are_collapsed_click_to_expand)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(),
                            )
                        }
                        if (section.isExpanded) {
                            items(
                                items = section.codes,
                                key = { code -> "code_${code.id}" },
                            ) {
                                VaultVerificationCodeItem(
                                    authCode = it.authCode,
                                    primaryLabel = it.title,
                                    secondaryLabel = it.subtitle,
                                    periodSeconds = it.periodSeconds,
                                    timeLeftSeconds = it.timeLeftSeconds,
                                    alertThresholdSeconds = it.alertThresholdSeconds,
                                    startIcon = it.startIcon,
                                    onItemClick = { onItemClick(it.authCode) },
                                    onDropdownMenuClick = { action ->
                                        onDropdownMenuClick(action, it)
                                    },
                                    showMoveToBitwarden = it.showMoveToBitwarden,
                                    allowLongPress = it.allowLongPressActions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                SharedCodesDisplayState.Error -> {
                    item(key = "shared_codes_error") {
                        Text(
                            text = stringResource(R.string.shared_codes_error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(),
                        )
                    }
                }
            }

            // Add a spacer item to prevent the FAB from hiding verification codes at the
            // bottom of the list
            item {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

/**
 * Displays the item listing screen with no existing items.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyItemListingContent(
    modifier: Modifier = Modifier,
    actionCardState: ItemListingState.ActionCardState,
    appTheme: AppTheme,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    ),
    onAddCodeClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onEnterSetupKeyClick: () -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
) {
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AuthenticatorTopAppBar(
                title = stringResource(id = R.string.verification_codes),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
                actions = { },
            )
        },
        floatingActionButton = {
            ExpandableFloatingActionButton(
                modifier = Modifier
                    .semantics { testTag = "AddItemButton" }
                    .padding(bottom = 16.dp),
                label = R.string.add_item.asText(),
                items = listOf(
                    ItemListingExpandableFabAction.ScanQrCode(
                        label = R.string.scan_a_qr_code.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = BitwardenDrawable.ic_camera),
                            contentDescription = stringResource(id = R.string.scan_a_qr_code),
                            testTag = "ScanQRCodeButton",
                        ),
                        onScanQrCodeClick = onScanQrCodeClick,
                    ),
                    ItemListingExpandableFabAction.EnterSetupKey(
                        label = R.string.enter_key_manually.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = BitwardenDrawable.ic_keyboard),
                            contentDescription = stringResource(id = R.string.enter_key_manually),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onEnterSetupKeyClick = onEnterSetupKeyClick,
                    ),
                ),
                expandableFabIcon = ExpandableFabIcon(
                    iconData = IconResource(
                        iconPainter = painterResource(id = BitwardenDrawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        testTag = "AddItemButton",
                    ),
                    iconRotation = 45f,
                ),
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = when (actionCardState) {
                ItemListingState.ActionCardState.None -> Arrangement.Center
                ItemListingState.ActionCardState.DownloadBitwardenApp -> Arrangement.Top
                ItemListingState.ActionCardState.SyncWithBitwarden -> Arrangement.Top
            },
        ) {
            ActionCard(
                actionCardState = actionCardState,
                onDownloadBitwardenClick = onDownloadBitwardenClick,
                onDownloadBitwardenDismissClick = onDismissDownloadBitwardenClick,
                onSyncWithBitwardenClick = onSyncWithBitwardenClick,
                onSyncWithBitwardenDismissClick = onDismissSyncWithBitwardenClick,
                onSyncLearnMoreClick = onSyncLearnMoreClick,
            )

            // Add a spacer if an action card is showing:
            when (actionCardState) {
                ItemListingState.ActionCardState.None -> Unit
                ItemListingState.ActionCardState.DownloadBitwardenApp,
                ItemListingState.ActionCardState.SyncWithBitwarden,
                    -> Spacer(Modifier.height(16.dp))
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(
                        id = when (appTheme) {
                            AppTheme.DARK -> BitwardenDrawable.ic_empty_vault_dark
                            AppTheme.LIGHT -> BitwardenDrawable.ic_empty_vault_light
                            AppTheme.DEFAULT -> BitwardenDrawable.ic_empty_vault
                        },
                    ),
                    contentDescription = stringResource(
                        id = R.string.empty_item_list,
                    ),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.you_dont_have_items_to_display),
                    style = Typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.empty_item_list_instruction),
                )

                Spacer(modifier = Modifier.height(16.dp))
                AuthenticatorFilledTonalButton(
                    modifier = Modifier
                        .semantics { testTag = "AddCodeButton" }
                        .fillMaxWidth(),
                    label = stringResource(R.string.add_code),
                    onClick = onAddCodeClick,
                )
            }
        }
    }
}

@Composable
private fun DownloadBitwardenActionCard(
    modifier: Modifier = Modifier,
    onDismissClick: () -> Unit,
    onDownloadBitwardenClick: () -> Unit,
) = BitwardenActionCard(
    modifier = modifier,
    actionIcon = rememberVectorPainter(BitwardenDrawable.ic_shield),
    actionText = stringResource(R.string.download_bitwarden_card_message),
    callToActionText = stringResource(R.string.download_now),
    titleText = stringResource(R.string.download_bitwarden_card_title),
    onCardClicked = onDownloadBitwardenClick,
    trailingContent = {
        IconButton(
            onClick = onDismissClick,
        ) {
            Icon(
                painter = painterResource(id = BitwardenDrawable.ic_close),
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp),
            )
        }
    },
)

@Suppress("LongMethod")
@Composable
private fun SyncWithBitwardenActionCard(
    modifier: Modifier = Modifier,
    onDismissClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(size = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Spacer(Modifier.height(height = 4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(width = 16.dp))
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_shield),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size = 20.dp),
                )
                Spacer(Modifier.width(width = 16.dp))
                Text(
                    text = stringResource(id = R.string.sync_with_the_bitwarden_app),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(weight = 1f))
            Spacer(Modifier.width(width = 16.dp))
            IconButton(onClick = onDismissClick) {
                Icon(
                    painter = painterResource(id = BitwardenDrawable.ic_close),
                    contentDescription = stringResource(id = R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size = 24.dp),
                )
            }
            Spacer(Modifier.width(width = 4.dp))
        }
        Text(
            text = stringResource(id = R.string.sync_with_bitwarden_action_card_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(start = 36.dp, end = 48.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(height = 16.dp))
        AuthenticatorFilledButton(
            label = stringResource(id = R.string.take_me_to_app_settings),
            onClick = onAppSettingsClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        AuthenticatorTextButton(
            label = stringResource(id = R.string.learn_more),
            onClick = onLearnMoreClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(height = 4.dp))
    }
}

@Composable
private fun ActionCard(
    actionCardState: ItemListingState.ActionCardState,
    onDownloadBitwardenClick: () -> Unit,
    onDownloadBitwardenDismissClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onSyncWithBitwardenDismissClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (actionCardState) {
        ItemListingState.ActionCardState.DownloadBitwardenApp -> {
            DownloadBitwardenActionCard(
                modifier = modifier,
                onDownloadBitwardenClick = onDownloadBitwardenClick,
                onDismissClick = onDownloadBitwardenDismissClick,
            )
        }

        ItemListingState.ActionCardState.SyncWithBitwarden -> {
            SyncWithBitwardenActionCard(
                modifier = modifier,
                onAppSettingsClick = onSyncWithBitwardenClick,
                onDismissClick = onSyncWithBitwardenDismissClick,
                onLearnMoreClick = onSyncLearnMoreClick,
            )
        }

        ItemListingState.ActionCardState.None -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
private fun EmptyListingContentPreview() {
    EmptyItemListingContent(
        modifier = Modifier.padding(horizontal = 16.dp),
        appTheme = AppTheme.DEFAULT,
        onAddCodeClick = { },
        onScanQrCodeClick = { },
        onEnterSetupKeyClick = { },
        actionCardState = ItemListingState.ActionCardState.DownloadBitwardenApp,
        onDownloadBitwardenClick = { },
        onDismissDownloadBitwardenClick = { },
        onSyncWithBitwardenClick = { },
        onSyncLearnMoreClick = { },
        onDismissSyncWithBitwardenClick = { },
    )
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
private fun ContentPreview() {
    BitwardenTheme {
        ItemListingContent(
            state = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = listOf(
                    VerificationCodeDisplayItem(
                        id = "",
                        title = "Local item",
                        subtitle = "with a subtitle",
                        timeLeftSeconds = 20,
                        periodSeconds = 30,
                        alertThresholdSeconds = 15,
                        authCode = "123456",
                        favorite = false,
                        showMoveToBitwarden = true,
                        allowLongPressActions = true,
                    ),
                ),
                sharedItems = SharedCodesDisplayState.Codes(
                    sections = listOf(
                        SharedCodesDisplayState.SharedCodesAccountSection(
                            id = "id",
                            label =
                                "longemailaddress+verification+codes@email.com | Bitawrden.eu (1)"
                                    .asText(),
                            codes = listOf(
                                VerificationCodeDisplayItem(
                                    id = "",
                                    title = "Shared item",
                                    subtitle = "with a subtitle",
                                    timeLeftSeconds = 15,
                                    periodSeconds = 30,
                                    alertThresholdSeconds = 15,
                                    authCode = "123456",
                                    favorite = false,
                                    showMoveToBitwarden = false,
                                    allowLongPressActions = false,
                                ),
                            ),
                            isExpanded = true,
                        ),
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                rememberTopAppBarState(),
            ),
            onNavigateToSearch = { },
            onScanQrCodeClick = { },
            onEnterSetupKeyClick = { },
            onItemClick = { },
            onDropdownMenuClick = { _, _ -> },
            onDownloadBitwardenClick = { },
            onDismissDownloadBitwardenClick = { },
            onSyncWithBitwardenClick = { },
            onDismissSyncWithBitwardenClick = { },
            onSyncLearnMoreClick = { },
            onSectionExpandedClick = { },
        )
    }
}
