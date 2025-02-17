package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.inputFieldVisibilityToggleTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputLabel
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputTestTag
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenKeyboardType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenMessage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenTitle
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLogoutConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalFido2CompletionManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.crypto.Cipher

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
    fido2CompletionManager: Fido2CompletionManager = LocalFido2CompletionManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

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

            is VaultUnlockEvent.Fido2CredentialAssertionError -> {
                fido2CompletionManager.completeFido2Assertion(
                    result = Fido2CredentialAssertionResult.Error(event.message),
                )
            }

            is VaultUnlockEvent.Fido2GetCredentialsError -> {
                fido2CompletionManager.completeFido2GetCredentialRequest(
                    result = Fido2GetCredentialsResult.Error(message = event.message),
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
        )

        VaultUnlockState.VaultUnlockDialog.Loading -> BitwardenLoadingDialog(
            text = stringResource(id = R.string.loading),
        )

        VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported -> {
            BitwardenBasicDialog(
                title = stringResource(id = R.string.biometrics_no_longer_supported_title),
                message = stringResource(id = R.string.biometrics_no_longer_supported),
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
                    autoFocus = state.showKeyboard,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = remember(viewModel) {
                            { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
                        },
                    ),
                    supportingText = state.vaultUnlockType.unlockScreenMessage(),
                    passwordFieldTestTag = state.vaultUnlockType.unlockScreenInputTestTag,
                    cardStyle = CardStyle.Top(hasDivider = false),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(
                    id = R.string.logged_in_as_on,
                    state.email,
                    state.environmentUrl,
                ),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier
                    .testTag("UserAndEnvironmentDataLabel")
                    .standardHorizontalMargin()
                    .cardStyle(
                        cardStyle = if (state.hideInput) CardStyle.Full else CardStyle.Bottom,
                        paddingStart = 16.dp,
                        paddingEnd = 16.dp,
                        paddingTop = 0.dp,
                    )
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
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (state.showBiometricInvalidatedMessage) {
                Text(
                    text = stringResource(R.string.account_biometric_invalidated),
                    textAlign = TextAlign.Start,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.status.error,
                    modifier = Modifier.standardHorizontalMargin(),
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
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
