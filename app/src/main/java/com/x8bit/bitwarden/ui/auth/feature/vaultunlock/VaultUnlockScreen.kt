package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenLogoutConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Vault Unlock screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VaultUnlockScreen(
    viewModel: VaultUnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultUnlockEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var accountMenuVisible by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { !accountMenuVisible },
    )

    // Dynamic dialogs
    when (val dialog = state.dialog) {
        is VaultUnlockState.VaultUnlockDialog.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = dialog.message,
            ),
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(VaultUnlockAction.DismissDialog) }
            },
        )

        VaultUnlockState.VaultUnlockDialog.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(R.string.loading.asText()),
        )

        null -> Unit
    }

    // Static dialogs
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    if (showLogoutConfirmationDialog) {
        BitwardenLogoutConfirmationDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            onConfirmClick = remember(viewModel) {
                {
                    showLogoutConfirmationDialog = false
                    viewModel.trySendAction(
                        VaultUnlockAction.ConfirmLogoutClick,
                    )
                }
            },
        )
    }

    // Content
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.verify_master_password),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
                actions = {
                    BitwardenAccountActionItem(
                        initials = state.initials,
                        color = state.avatarColor,
                        onClick = { accountMenuVisible = !accountMenuVisible },
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.log_out),
                                onClick = { showLogoutConfirmationDialog = true },
                            ),
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        Box {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                BitwardenPasswordField(
                    label = stringResource(id = R.string.master_password),
                    value = state.passwordInput,
                    onValueChange = remember(viewModel) {
                        { viewModel.trySendAction(VaultUnlockAction.PasswordInputChanged(it)) }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(id = R.string.vault_locked_master_password),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        id = R.string.logged_in_as_on,
                        state.email,
                        state.environmentUrl,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                BitwardenFilledButton(
                    label = stringResource(id = R.string.unlock),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
                    },
                    isEnabled = state.passwordInput.isNotEmpty(),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.navigationBarsPadding())
            }

            BitwardenAccountSwitcher(
                isVisible = accountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultUnlockAction.SwitchAccountClick(it)) }
                },
                onLockAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultUnlockAction.LockAccountClick(it)) }
                },
                onLogoutAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(it)) }
                },
                onAddAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultUnlockAction.AddAccountClick) }
                },
                onDismissRequest = { accountMenuVisible = false },
                topAppBarScrollBehavior = scrollBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
