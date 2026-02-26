package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.bitwarden.ui.platform.components.account.dialog.BitwardenLogoutConfirmationDialog
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.support.BitwardenSupportingContent
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.inputFieldVisibilityToggleTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputLabel
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenKeyboardType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenMessage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenTitle
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalCredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import javax.crypto.Cipher

/**
 * Time slice to delay auto-focusing on the password/pin field. Because of the refresh that
 * takes place when switching accounts or changing the lock status we want to delay this
 * longer than the delay in place for sending those actions in [com.x8bit.bitwarden.MainViewModel]
 * defined by `ANIMATION_REFRESH_DELAY`. We need to  ensure this value is
 * always greater.
 */
private const val AUTO_FOCUS_DELAY = 575L

/**
 * The top level composable for the Vault Unlock screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultUnlockScreen(
    viewModel: VaultUnlockViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    focusManager: FocusManager = LocalFocusManager.current,
    credentialProviderCompletionManager: CredentialProviderCompletionManager =
        LocalCredentialProviderCompletionManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(state.requiresBiometricsLogin) {
        if (state.requiresBiometricsLogin && !biometricsManager.isBiometricsSupported) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsNoLongerSupported)
        }
    }

    val onBiometricsUnlockSuccess: (cipher: Cipher) -> Unit = remember(viewModel) {
        { viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(it)) }
    }
    val onBiometricsLockOut: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut) }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
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

            is VaultUnlockEvent.Fido2CredentialAssertionError -> {
                credentialProviderCompletionManager.completeFido2Assertion(
                    result = AssertFido2CredentialResult.Error(message = event.message),
                )
            }

            is VaultUnlockEvent.GetCredentialsError -> {
                credentialProviderCompletionManager.completeProviderGetCredentialsRequest(
                    result = GetCredentialsResult.Error(message = event.message),
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
            title = dialog.title(),
            message = dialog.message(),
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(VaultUnlockAction.DismissDialog) }
            },
            throwable = dialog.throwable,
        )

        VaultUnlockState.VaultUnlockDialog.Loading -> BitwardenLoadingDialog(
            text = stringResource(id = BitwardenString.loading),
        )

        VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.biometrics_no_longer_supported_title),
                message = stringResource(id = BitwardenString.biometrics_no_longer_supported),
                onDismissRequest = remember {
                    {
                        viewModel.trySendAction(
                            VaultUnlockAction.DismissBiometricsNoLongerSupportedDialog,
                        )
                    }
                },
            )
        }

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
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { focusManager.clearFocus(force = true) },
                )
            },
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
                            onClick = {
                                focusManager.clearFocus()
                                accountMenuVisible = !accountMenuVisible
                            },
                        )
                    }
                    BitwardenOverflowActionItem(
                        contentDescription = stringResource(BitwardenString.more),
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = BitwardenString.log_out),
                                onClick = { showLogoutConfirmationDialog = true },
                            ),
                        ),
                    )
                },
            )
        },
        overlay = {
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
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            if (!state.hideInput) {
                // When switching from an unlocked account to a locked account, the
                // current activity is recreated and therefore the composition takes place
                // twice. Adding this delay prevents the MP or Pin field
                // from auto focusing on the first composition which creates a visual jank where
                // the keyboard shows, disappears, and then shows again.
                var autoFocusDelayCompleted by rememberSaveable {
                    mutableStateOf(false)
                }
                LaunchedEffect(Unit) {
                    delay(AUTO_FOCUS_DELAY)
                    autoFocusDelayCompleted = true
                }
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
                    autoFocus = state.showKeyboard && autoFocusDelayCompleted,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = remember(viewModel) {
                            { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
                        },
                    ),
                    passwordFieldTestTag = state.vaultUnlockType.unlockScreenInputTestTag,
                    cardStyle = CardStyle.Top(),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            BitwardenSupportingContent(
                cardStyle = if (state.hideInput) CardStyle.Full else CardStyle.Bottom,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            ) {
                if (!state.hideInput) {
                    Text(
                        text = state.vaultUnlockType.unlockScreenMessage(),
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(height = 16.dp))
                }
                Text(
                    text = stringResource(
                        id = BitwardenString.logged_in_as_on,
                        formatArgs = arrayOf(state.email, state.environmentUrl),
                    ),
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier
                        .testTag(tag = "UserAndEnvironmentDataLabel")
                        .fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (state.showBiometricLogin && biometricsManager.isBiometricsSupported) {
                BitwardenOutlinedButton(
                    label = stringResource(id = BitwardenString.use_biometrics_to_unlock),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick) }
                    },
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (state.showBiometricInvalidatedMessage) {
                Text(
                    text = stringResource(BitwardenString.account_biometric_invalidated),
                    textAlign = TextAlign.Start,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.status.error,
                    modifier = Modifier.standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (!state.hideInput) {
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.unlock),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
                    },
                    isEnabled = state.input.isNotEmpty(),
                    modifier = Modifier
                        .testTag("UnlockVaultButton")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
