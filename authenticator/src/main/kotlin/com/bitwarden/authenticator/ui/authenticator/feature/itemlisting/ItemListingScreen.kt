package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.components.header.AuthenticatorExpandingHeader
import com.bitwarden.authenticator.ui.platform.composition.LocalPermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.util.startAuthenticatorAppSettings
import com.bitwarden.authenticator.ui.platform.util.startBitwardenAccountSettings
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.color.bitwardenCardColors
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.fab.BitwardenExpandableFloatingActionButton
import com.bitwarden.ui.platform.components.fab.model.ExpandableFabIcon
import com.bitwarden.ui.platform.components.fab.model.ExpandableFabOption
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.persistentListOf
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
    val resources = LocalResources.current
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
                Toast.makeText(context, event.message(resources), Toast.LENGTH_LONG).show()
            }

            is ItemListingEvent.NavigateToEditItem -> onNavigateToEditItemScreen(event.id)
            is ItemListingEvent.NavigateToAppSettings -> {
                intentManager.startAuthenticatorAppSettings()
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
                intentManager.startBitwardenAccountSettings()
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
            { viewModel.trySendAction(ItemListingAction.DialogDismiss) }
        },
        onConfirmDeleteClick = remember(viewModel) {
            { viewModel.trySendAction(ItemListingAction.ConfirmDeleteClick(it)) }
        },
    )

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = BitwardenString.verification_codes),
                scrollBehavior = scrollBehavior,
                actions = {
                    if (state.viewState is ItemListingState.ViewState.Content) {
                        BitwardenSearchActionItem(
                            contentDescription = stringResource(id = BitwardenString.search_codes),
                            onClick = onNavigateToSearch,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            BitwardenExpandableFloatingActionButton(
                modifier = Modifier.testTag("AddItemButton"),
                items = persistentListOf(
                    ExpandableFabOption(
                        label = BitwardenString.scan_a_qr_code.asText(),
                        icon = IconData.Local(
                            iconRes = BitwardenDrawable.ic_camera_small,
                            contentDescription = BitwardenString.scan_a_qr_code.asText(),
                            testTag = "ScanQRCodeButton",
                        ),
                        onFabOptionClick = remember(viewModel) {
                            { launcher.launch(Manifest.permission.CAMERA) }
                        },
                    ),
                    ExpandableFabOption(
                        label = BitwardenString.enter_key_manually.asText(),
                        icon = IconData.Local(
                            iconRes = BitwardenDrawable.ic_lock_encrypted_small,
                            contentDescription = BitwardenString.enter_key_manually.asText(),
                            testTag = "EnterSetupKeyButton",
                        ),
                        onFabOptionClick = remember(viewModel) {
                            { viewModel.trySendAction(ItemListingAction.EnterSetupKeyClick) }
                        },
                    ),
                ),
                expandableFabIcon = ExpandableFabIcon(
                    icon = IconData.Local(
                        iconRes = BitwardenDrawable.ic_plus,
                        contentDescription = BitwardenString.add_item.asText(),
                        testTag = "AddItemButton",
                    ),
                    iconRotation = 45f,
                ),
            )
        },
        snackbarHost = { FirstTimeSyncSnackbarHost(state = snackbarHostState) },
    ) {
        when (val currentState = state.viewState) {
            is ItemListingState.ViewState.Content -> {
                ItemListingContent(
                    state = currentState,
                    onItemClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.ItemClick(it)) }
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
                        { viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick) }
                    },
                    onDismissDownloadBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss) }
                    },
                    onSyncWithBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
                    },
                    onDismissSyncWithBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss) }
                    },
                    onSyncLearnMoreClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
                    },
                    onSectionExpandedClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SectionExpandedClick(it)) }
                    },
                )
            }

            ItemListingState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }

            is ItemListingState.ViewState.NoItems -> {
                EmptyItemListingContent(
                    actionCardState = currentState.actionCard,
                    appTheme = state.appTheme,
                    onAddCodeClick = remember(viewModel) {
                        { launcher.launch(Manifest.permission.CAMERA) }
                    },
                    onDownloadBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick) }
                    },
                    onDismissDownloadBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss) }
                    },
                    onSyncWithBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
                    },
                    onSyncLearnMoreClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
                    },
                    onDismissSyncWithBitwardenClick = remember(viewModel) {
                        { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss) }
                    },
                )
            }
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
                text = stringResource(id = BitwardenString.syncing),
            )
        }

        is ItemListingState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title(),
                message = dialog.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is ItemListingState.DialogState.DeleteConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.delete),
                message = dialog.message(),
                confirmButtonText = stringResource(id = BitwardenString.okay),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
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
@Composable
private fun ItemListingContent(
    state: ItemListingState.ViewState.Content,
    onItemClick: (String) -> Unit,
    onDropdownMenuClick: (VaultDropdownMenuAction, VerificationCodeDisplayItem) -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    onSectionExpandedClick: (SharedCodesDisplayState.SharedCodesAccountSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLocalHeaderExpanded by rememberSaveable { mutableStateOf(value = true) }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "action_card") {
            ActionCard(
                actionCardState = state.actionCard,
                onDownloadBitwardenClick = onDownloadBitwardenClick,
                onDownloadBitwardenDismissClick = onDismissDownloadBitwardenClick,
                onSyncWithBitwardenClick = onSyncWithBitwardenClick,
                onSyncWithBitwardenDismissClick = onDismissSyncWithBitwardenClick,
                onSyncLearnMoreClick = onSyncLearnMoreClick,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(top = 12.dp, bottom = 16.dp)
                    .animateItem(),
            )
        }
        if (state.favoriteItems.isNotEmpty()) {
            item(key = "favorites_header") {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.favorites),
                    supportingLabel = state.favoriteItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(
                items = state.favoriteItems,
                key = { _, it -> "favorite_item_${it.id}" },
            ) { index, it ->
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
                    cardStyle = state.favoriteItems.toListItemCardStyle(index = index),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
        }

        if (state.shouldShowLocalHeader) {
            item(key = "local_items_header") {
                AuthenticatorExpandingHeader(
                    label = stringResource(
                        id = BitwardenString.local_codes,
                        state.itemList.size,
                    ),
                    isExpanded = isLocalHeaderExpanded,
                    onClick = { isLocalHeaderExpanded = !isLocalHeaderExpanded },
                    onClickLabel = stringResource(
                        id = if (isLocalHeaderExpanded) {
                            BitwardenString.local_items_are_expanded_click_to_collapse
                        } else {
                            BitwardenString.local_items_are_collapsed_click_to_expand
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        if (isLocalHeaderExpanded) {
            itemsIndexed(
                items = state.itemList,
                key = { _, it -> "local_item_${it.id}" },
            ) { index, it ->
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
                    cardStyle = state.itemList.toListItemCardStyle(index = index),
                    modifier = Modifier
                        .standardHorizontalMargin()
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
                            onClickLabel = stringResource(
                                id = if (section.isExpanded) {
                                    BitwardenString.items_expanded_click_to_collapse
                                } else {
                                    BitwardenString.items_are_collapsed_click_to_expand
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .standardHorizontalMargin()
                                .animateItem(),
                        )
                    }
                    if (section.isExpanded) {
                        itemsIndexed(
                            items = section.codes,
                            key = { _, code -> "code_${code.id}" },
                        ) { index, it ->
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
                                cardStyle = section.codes.toListItemCardStyle(index = index),
                                modifier = Modifier
                                    .standardHorizontalMargin()
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
                        text = stringResource(BitwardenString.shared_codes_error),
                        color = BitwardenTheme.colorScheme.text.secondary,
                        style = BitwardenTheme.typography.bodySmall,
                        modifier = Modifier
                            .standardHorizontalMargin()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }

        // Add a spacer item to prevent the FAB from hiding verification codes at the
        // bottom of the list
        item {
            Spacer(modifier = Modifier.height(height = 88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Displays the item listing screen with no existing items.
 */
@Suppress("LongMethod")
@Composable
fun EmptyItemListingContent(
    actionCardState: ItemListingState.ActionCardState,
    appTheme: AppTheme,
    onAddCodeClick: () -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    onDismissDownloadBitwardenClick: () -> Unit,
    onSyncWithBitwardenClick: () -> Unit,
    onSyncLearnMoreClick: () -> Unit,
    onDismissSyncWithBitwardenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            modifier = Modifier
                .standardHorizontalMargin()
                .padding(top = 12.dp, bottom = 16.dp),
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .standardHorizontalMargin(),
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
                    id = BitwardenString.empty_item_list,
                ),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = BitwardenString.you_dont_have_items_to_display),
                style = BitwardenTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(id = BitwardenString.empty_item_list_instruction),
            )

            Spacer(modifier = Modifier.height(16.dp))
            BitwardenFilledButton(
                modifier = Modifier
                    .testTag("AddCodeButton")
                    .fillMaxWidth(),
                label = stringResource(BitwardenString.add_code),
                onClick = onAddCodeClick,
            )

            Spacer(modifier = Modifier.height(height = 12.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DownloadBitwardenActionCard(
    onDismissClick: () -> Unit,
    onDownloadBitwardenClick: () -> Unit,
    modifier: Modifier = Modifier,
) = BitwardenActionCard(
    modifier = modifier,
    cardSubtitle = stringResource(BitwardenString.download_bitwarden_card_message),
    actionText = stringResource(BitwardenString.download_now),
    cardTitle = stringResource(BitwardenString.download_bitwarden_card_title),
    onActionClick = onDownloadBitwardenClick,
    leadingContent = {
        Icon(
            painter = rememberVectorPainter(BitwardenDrawable.ic_shield),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.secondary,
        )
    },
    onDismissClick = onDismissClick,
)

@Suppress("LongMethod")
@Composable
private fun SyncWithBitwardenActionCard(
    onDismissClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = BitwardenTheme.shapes.actionCard,
        colors = bitwardenCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 1.dp, color = BitwardenTheme.colorScheme.stroke.border),
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
                    tint = BitwardenTheme.colorScheme.icon.secondary,
                    modifier = Modifier.size(size = 20.dp),
                )
                Spacer(Modifier.width(width = 16.dp))
                Text(
                    text = stringResource(id = BitwardenString.sync_with_the_bitwarden_app),
                    style = BitwardenTheme.typography.bodyLarge,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
            Spacer(Modifier.weight(weight = 1f))
            Spacer(Modifier.width(width = 16.dp))
            IconButton(onClick = onDismissClick) {
                Icon(
                    painter = painterResource(id = BitwardenDrawable.ic_close),
                    contentDescription = stringResource(id = BitwardenString.close),
                    tint = BitwardenTheme.colorScheme.icon.primary,
                    modifier = Modifier.size(size = 24.dp),
                )
            }
            Spacer(Modifier.width(width = 4.dp))
        }
        Text(
            text = stringResource(id = BitwardenString.sync_with_bitwarden_action_card_message),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(start = 36.dp, end = 48.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(height = 16.dp))
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.take_me_to_app_settings),
            onClick = onAppSettingsClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        BitwardenTextButton(
            label = stringResource(id = BitwardenString.learn_more),
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

@Composable
@Preview(showBackground = true)
private fun EmptyListingContentPreview() {
    EmptyItemListingContent(
        modifier = Modifier.padding(horizontal = 16.dp),
        appTheme = AppTheme.DEFAULT,
        onAddCodeClick = { },
        actionCardState = ItemListingState.ActionCardState.DownloadBitwardenApp,
        onDownloadBitwardenClick = { },
        onDismissDownloadBitwardenClick = { },
        onSyncWithBitwardenClick = { },
        onSyncLearnMoreClick = { },
        onDismissSyncWithBitwardenClick = { },
    )
}

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
