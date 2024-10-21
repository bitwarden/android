package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.authenticator.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.fab.ExpandableFabIcon
import com.bitwarden.authenticator.ui.platform.components.fab.ExpandableFloatingActionButton
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderTextWithSupportLabel
import com.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.theme.LocalPermissionsManager
import com.bitwarden.authenticator.ui.platform.theme.Typography

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
    var shouldShowPermissionDialog by rememberSaveable { mutableStateOf(false) }
    val launcher = permissionsManager.getLauncher { isGranted ->
        if (isGranted) {
            viewModel.trySendAction(ItemListingAction.ScanQrCodeClick)
        } else {
            shouldShowPermissionDialog = true
        }
    }

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

            ItemListingEvent.NavigateToBitwardenSettings -> {
                intentManager.startMainBitwardenAppAccountSettings()
            }
        }
    }

    if (shouldShowPermissionDialog) {
        BitwardenTwoButtonDialog(
            message = stringResource(id = R.string.enable_camera_permission_to_use_the_scanner),
            confirmButtonText = stringResource(id = R.string.settings),
            dismissButtonText = stringResource(id = R.string.no_thanks),
            onConfirmClick = remember(viewModel) {
                { viewModel.trySendAction(ItemListingAction.SettingsClick) }
            },
            onDismissClick = { shouldShowPermissionDialog = false },
            onDismissRequest = { shouldShowPermissionDialog = false },
            title = null,
        )
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
                currentState,
                scrollBehavior,
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
                onEditItemClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            ItemListingAction.EditItemClick(it),
                        )
                    }
                },
                onDeleteItemClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            ItemListingAction.DeleteItemClick(it),
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
                onScanQuCodeClick = remember(viewModel) {
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
                confirmButtonText = stringResource(id = R.string.ok),
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
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateToSearch: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onEnterSetupKeyClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onEditItemClick: (String) -> Unit,
    onDeleteItemClick: (String) -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
) {
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.verification_codes),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenSearchActionItem(
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
                            iconPainter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(id = R.string.scan_a_qr_code),
                            testTag = "ScanQRCodeButton",
                        ),
                        onScanQrCodeClick = onScanQrCodeClick,
                    ),
                    ItemListingExpandableFabAction.EnterSetupKey(
                        label = R.string.enter_key_manually.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_keyboard_24px),
                            contentDescription = stringResource(id = R.string.enter_key_manually),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onEnterSetupKeyClick = onEnterSetupKeyClick,
                    ),
                ),
                expandableFabIcon = ExpandableFabIcon(
                    iconData = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        testTag = "AddItemButton",
                    ),
                    iconRotation = 45f,
                ),
            )
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn {
                item {
                    when (state.actionCard) {
                        ItemListingState.ActionCardState.DownloadBitwardenApp ->
                            DownloadBitwardenActionCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onDownloadBitwardenClick = onDownloadBitwardenClick,
                                onDismissClick = onDismissDownloadBitwardenClick,
                            )

                        ItemListingState.ActionCardState.SyncWithBitwarden ->
                            SyncWithBitwardenActionCard(
                                modifier = Modifier.padding(16.dp),
                                onSyncWithBitwardenClick = onSyncWithBitwardenClick,
                                onDismissClick = onDismissSyncWithBitwardenClick,
                            )

                        ItemListingState.ActionCardState.None -> Unit
                    }
                }
                if (state.favoriteItems.isNotEmpty()) {
                    item {
                        BitwardenListHeaderTextWithSupportLabel(
                            label = stringResource(id = R.string.favorites),
                            supportingLabel = state.favoriteItems.count().toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(state.favoriteItems) {
                        VaultVerificationCodeItem(
                            authCode = it.authCode,
                            primaryLabel = it.issuer,
                            secondaryLabel = it.label,
                            periodSeconds = it.periodSeconds,
                            timeLeftSeconds = it.timeLeftSeconds,
                            alertThresholdSeconds = it.alertThresholdSeconds,
                            startIcon = it.startIcon,
                            onItemClick = { onItemClick(it.authCode) },
                            onEditItemClick = { onEditItemClick(it.id) },
                            onDeleteItemClick = { onDeleteItemClick(it.id) },
                            allowLongPress = it.allowLongPressActions,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(all = 16.dp),
                        )
                    }
                }

                items(state.itemList) {
                    VaultVerificationCodeItem(
                        authCode = it.authCode,
                        primaryLabel = it.issuer,
                        secondaryLabel = it.label,
                        periodSeconds = it.periodSeconds,
                        timeLeftSeconds = it.timeLeftSeconds,
                        alertThresholdSeconds = it.alertThresholdSeconds,
                        startIcon = it.startIcon,
                        onItemClick = { onItemClick(it.authCode) },
                        onEditItemClick = { onEditItemClick(it.id) },
                        onDeleteItemClick = { onDeleteItemClick(it.id) },
                        allowLongPress = it.allowLongPressActions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // If there are any items in the local lists, add a spacer between
                // local codes and shared codes:
                if (state.itemList.isNotEmpty() || state.favoriteItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                when (state.sharedItems) {
                    is SharedCodesDisplayState.Codes -> {
                        items(state.sharedItems.sections) { section ->
                            BitwardenListHeaderText(
                                label = section.label(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )
                            section.codes.forEach {
                                VaultVerificationCodeItem(
                                    authCode = it.authCode,
                                    primaryLabel = it.issuer,
                                    secondaryLabel = it.label,
                                    periodSeconds = it.periodSeconds,
                                    timeLeftSeconds = it.timeLeftSeconds,
                                    alertThresholdSeconds = it.alertThresholdSeconds,
                                    startIcon = it.startIcon,
                                    onItemClick = { onItemClick(it.authCode) },
                                    onEditItemClick = { },
                                    onDeleteItemClick = { },
                                    allowLongPress = it.allowLongPressActions,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    SharedCodesDisplayState.Error -> item {
                        Text(
                            text = stringResource(R.string.shared_codes_error),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
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
    onScanQuCodeClick: () -> Unit,
    onEnterSetupKeyClick: () -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
) {
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
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
                            iconPainter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(id = R.string.scan_a_qr_code),
                            testTag = "ScanQRCodeButton",
                        ),
                        onScanQrCodeClick = onScanQuCodeClick,
                    ),
                    ItemListingExpandableFabAction.EnterSetupKey(
                        label = R.string.enter_key_manually.asText(),
                        icon = IconResource(
                            iconPainter = painterResource(id = R.drawable.ic_keyboard_24px),
                            contentDescription = stringResource(id = R.string.enter_key_manually),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onEnterSetupKeyClick = onEnterSetupKeyClick,
                    ),
                ),
                expandableFabIcon = ExpandableFabIcon(
                    iconData = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_plus),
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
            when (actionCardState) {
                ItemListingState.ActionCardState.DownloadBitwardenApp ->
                    DownloadBitwardenActionCard(
                        modifier = Modifier.padding(16.dp),
                        onDismissClick = onDismissDownloadBitwardenClick,
                        onDownloadBitwardenClick = onDownloadBitwardenClick,
                    )

                ItemListingState.ActionCardState.SyncWithBitwarden ->
                    SyncWithBitwardenActionCard(
                        modifier = Modifier.padding(16.dp),
                        onDismissClick = onDismissSyncWithBitwardenClick,
                        onSyncWithBitwardenClick = onSyncWithBitwardenClick,
                    )

                ItemListingState.ActionCardState.None -> Unit
            }

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
                            AppTheme.DARK -> R.drawable.ic_empty_vault_dark
                            AppTheme.LIGHT -> R.drawable.ic_empty_vault_light
                            AppTheme.DEFAULT -> R.drawable.ic_empty_vault
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
                BitwardenFilledTonalButton(
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
    actionIcon = rememberVectorPainter(R.drawable.ic_bitwarden),
    actionText = stringResource(R.string.download_bitwarden_card_message),
    callToActionText = stringResource(R.string.download),
    titleText = stringResource(R.string.download_bitwarden_card_title),
    onCardClicked = onDownloadBitwardenClick,
    trailingContent = {
        IconButton(
            onClick = onDismissClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp),
            )
        }
    },
)

@Composable
private fun SyncWithBitwardenActionCard(
    modifier: Modifier = Modifier,
    onDismissClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
) = BitwardenActionCard(
    modifier = modifier,
    actionIcon = rememberVectorPainter(R.drawable.ic_refresh),
    actionText = stringResource(R.string.sync_with_bitwarden_action_card_message),
    callToActionText = stringResource(R.string.go_to_settings),
    titleText = stringResource(R.string.sync_with_bitwarden_app),
    onCardClicked = onSyncWithBitwardenClick,
    trailingContent = {
        IconButton(
            onClick = onDismissClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp),
            )
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
private fun EmptyListingContentPreview() {
    EmptyItemListingContent(
        modifier = Modifier.padding(horizontal = 16.dp),
        appTheme = AppTheme.DEFAULT,
        onAddCodeClick = { },
        onScanQuCodeClick = { },
        onEnterSetupKeyClick = { },
        actionCardState = ItemListingState.ActionCardState.DownloadBitwardenApp,
        onDownloadBitwardenClick = { },
        onDismissDownloadBitwardenClick = { },
        onSyncWithBitwardenClick = { },
        onDismissSyncWithBitwardenClick = { },
    )
}
