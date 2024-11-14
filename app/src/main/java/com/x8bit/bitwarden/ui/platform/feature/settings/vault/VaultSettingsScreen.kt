package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.badge.NotificationBadge
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.x8bit.bitwarden.ui.platform.components.snackbar.rememberBitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay

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
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
    viewModel: VaultSettingsViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val snackbarHostState = rememberBitwardenSnackbarHostState()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultSettingsEvent.NavigateBack -> onNavigateBack()
            VaultSettingsEvent.NavigateToExportVault -> onNavigateToExportVault()
            VaultSettingsEvent.NavigateToFolders -> onNavigateToFolders()
            is VaultSettingsEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }

            is VaultSettingsEvent.NavigateToImportVault -> {
                if (state.isNewImportLoginsFlowEnabled) {
                    onNavigateToImportLogins(SnackbarRelay.VAULT_SETTINGS_RELAY)
                } else {
                    intentManager.launchUri(event.url.toUri())
                }
            }

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
                title = stringResource(id = R.string.vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
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
            AnimatedVisibility(
                visible = state.shouldShowImportCard,
                label = "ImportLoginsActionCard",
                exit = actionCardExitAnimation(),
            ) {
                BitwardenActionCard(
                    cardTitle = stringResource(id = R.string.import_saved_logins),
                    actionText = stringResource(R.string.get_started),
                    cardSubtitle = stringResource(R.string.use_a_computer_to_import_logins),
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
                        .padding(top = 12.dp, bottom = 16.dp),
                )
            }
            BitwardenTextRow(
                text = stringResource(R.string.folders),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.FoldersButtonClick) }
                },
                withDivider = true,
                modifier = Modifier
                    .testTag("FoldersLabel")
                    .fillMaxWidth(),
            )

            BitwardenTextRow(
                text = stringResource(R.string.export_vault),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultSettingsAction.ExportVaultClick) }
                },
                withDivider = true,
                modifier = Modifier
                    .testTag("ExportVaultLabel")
                    .fillMaxWidth(),
            )

            if (state.isNewImportLoginsFlowEnabled) {
                BitwardenTextRow(
                    text = stringResource(R.string.import_items),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultSettingsAction.ImportItemsClick) }
                    },
                    withDivider = true,
                    modifier = Modifier
                        .testTag("ImportItemsLinkItemView")
                        .fillMaxWidth(),
                )
            } else {
                BitwardenExternalLinkRow(
                    text = stringResource(R.string.import_items),
                    onConfirmClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultSettingsAction.ImportItemsClick) }
                    },
                    withDivider = true,
                    dialogTitle = stringResource(id = R.string.continue_to_web_app),
                    dialogMessage =
                    stringResource(
                        id = R.string.you_can_import_data_to_your_vault_on_x,
                        state.importUrl,
                    ),
                    modifier = Modifier
                        .testTag("ImportItemsLinkItemView")
                        .fillMaxWidth(),
                )
            }
        }
    }
}
