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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.inputFieldVisibilityToggleTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputLabel
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenKeyboardType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenMessage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenTitle
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLogoutConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.crypto.Cipher

/**
 * The top level composable for the Vault Unlock screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VaultUnlockScreen(
    viewModel: VaultUnlockViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

    val onBiometricsUnlockSuccess: (cipher: Cipher?) -> Unit = remember(viewModel) {
        { viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(it)) }
    }
    val onBiometricsLockOut: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut) }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultUnlockEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultUnlockEvent.PromptForBiometrics -> {
                biometricsManager.promptBiometrics(
                    onSuccess = onBiometricsUnlockSuccess,
                    onCancel = {
                        // no-op
                    },
                    onError = {
                        // no-op
                    },
                    onLockOut = onBiometricsLockOut,
                    cipher = event.cipher,
                )
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
                title = state.vaultUnlockType.unlockScreenTitle(),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
                actions = {
                    if (state.showAccountMenu) {
                        BitwardenAccountActionItem(
                            initials = state.initials,
                            color = state.avatarColor,
                            onClick = { accountMenuVisible = !accountMenuVisible },
                        )
                    }
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
                if (!state.hideInput) {
                    BitwardenPasswordField(
                        label = state.vaultUnlockType.unlockScreenInputLabel(),
                        value = state.input,
                        onValueChange = remember(viewModel) {
                            { viewModel.trySendAction(VaultUnlockAction.InputChanged(it)) }
                        },
                        keyboardType = state.vaultUnlockType.unlockScreenKeyboardType,
                        showPasswordTestTag = state
                            .vaultUnlockType
                            .inputFieldVisibilityToggleTestTag,
                        modifier = Modifier
                            .testTag(state.vaultUnlockType.unlockScreenInputTestTag)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = state.vaultUnlockType.unlockScreenMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(
                        id = R.string.logged_in_as_on,
                        state.email,
                        state.environmentUrl,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .testTag("UserAndEnvironmentDataLabel")
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (state.showBiometricLogin && biometricsManager.isBiometricsSupported) {
                    BitwardenOutlinedButton(
                        label = stringResource(id = R.string.use_biometrics_to_unlock),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick) }
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (!state.hideInput) {
                    BitwardenFilledButton(
                        label = stringResource(id = R.string.unlock),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
                        },
                        isEnabled = state.input.isNotEmpty(),
                        modifier = Modifier
                            .testTag("UnlockVaultButton")
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                }
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
