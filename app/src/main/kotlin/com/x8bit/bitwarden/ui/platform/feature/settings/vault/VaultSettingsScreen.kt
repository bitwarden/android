package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the vault settings screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToImportItems: () -> Unit,
    viewModel: VaultSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultSettingsEvent.NavigateBack -> onNavigateBack()
            VaultSettingsEvent.NavigateToExportVault -> onNavigateToExportVault()
            VaultSettingsEvent.NavigateToFolders -> onNavigateToFolders()
            is VaultSettingsEvent.NavigateToImportVault -> onNavigateToImportLogins()
            is VaultSettingsEvent.NavigateToImportItems -> onNavigateToImportItems()
            is VaultSettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.BackClick) }
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(
                bitwardenHostState = snackbarHostState,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(
                visible = state.showImportActionCard,
                label = "ImportLoginsActionCard",
                exit = actionCardExitAnimation(),
            ) {
                BitwardenActionCard(
                    cardTitle = stringResource(id = BitwardenString.import_saved_logins),
                    actionText = stringResource(BitwardenString.get_started),
                    cardSubtitle = stringResource(BitwardenString.use_a_computer_to_import_logins),
                    onActionClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardCtaClick)
                        }
                    },
                    onDismissClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                VaultSettingsAction.ImportLoginsCardDismissClick,
                            )
                        }
                    },
                    leadingContent = {
                        NotificationBadge(notificationCount = 1)
                    },
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .padding(bottom = 16.dp),
                )
            }
            BitwardenTextRow(
                text = stringResource(BitwardenString.folders),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.FoldersButtonClick) }
                },
                withDivider = false,
                cardStyle = CardStyle.Top(),
                modifier = Modifier
                    .testTag("FoldersLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            BitwardenTextRow(
                text = stringResource(BitwardenString.export_vault),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.ExportVaultClick) }
                },
                withDivider = false,
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("ExportVaultLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            BitwardenTextRow(
                text = stringResource(BitwardenString.import_items),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.ImportItemsClick) }
                },
                withDivider = false,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag("ImportItemsLinkItemView")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            ) {
                if (state.showImportItemsChevron) {
                    Icon(
                        painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_right),
                        contentDescription = null,
                        tint = BitwardenTheme.colorScheme.icon.primary,
                        modifier = Modifier
                            .mirrorIfRtl()
                            .size(size = 16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
