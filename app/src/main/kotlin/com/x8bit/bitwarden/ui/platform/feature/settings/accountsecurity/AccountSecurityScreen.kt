package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.account.dialog.BitwardenLogoutConfirmationDialog
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.dropdown.BitwardenTimePickerButton
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.support.BitwardenSupportingText
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithBiometricsSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithPinSwitch
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricSupportStatus
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import com.x8bit.bitwarden.ui.platform.util.minutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.crypto.Cipher

/**
 * Displays the account security screen.
 */
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    var showBiometricsPrompt by rememberSaveable { mutableStateOf(false) }
    val unlockWithBiometricToggle: (cipher: Cipher) -> Unit = remember(viewModel) {
        {
            viewModel.trySendAction(
                action = AccountSecurityAction.UnlockWithBiometricToggleEnabled(cipher = it),
            )
        }
    }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AccountSecurityEvent.NavigateBack -> onNavigateBack()

            AccountSecurityEvent.NavigateToApplicationDataSettings -> {
                intentManager.startAppSettingsActivity()
            }

            AccountSecurityEvent.NavigateToDeleteAccount -> onNavigateToDeleteAccount()

            AccountSecurityEvent.NavigateToFingerprintPhrase -> {
                intentManager.launchUri("http://bitwarden.com/help/fingerprint-phrase".toUri())
            }

            AccountSecurityEvent.NavigateToPendingRequests -> onNavigateToPendingRequests()

            is AccountSecurityEvent.NavigateToTwoStepLogin -> {
                intentManager.launchUri(event.url.toUri())
            }

            is AccountSecurityEvent.NavigateToChangeMasterPassword -> {
                intentManager.launchUri(event.url.toUri())
            }

            is AccountSecurityEvent.ShowBiometricsPrompt -> {
                showBiometricsPrompt = true
                biometricsManager.promptBiometrics(
                    onSuccess = {
                        unlockWithBiometricToggle(it)
                        showBiometricsPrompt = false
                    },
                    onCancel = { showBiometricsPrompt = false },
                    onLockOut = { showBiometricsPrompt = false },
                    onError = { showBiometricsPrompt = false },
                    cipher = event.cipher,
                )
            }

            AccountSecurityEvent.NavigateToSetupUnlockScreen -> onNavigateToSetupUnlockScreen()
        }
    }

    AccountSecurityDialogs(
        state = state,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(AccountSecurityAction.DismissDialog) }
        },
        onConfirmLogoutClick = remember(viewModel) {
            { viewModel.trySendAction(AccountSecurityAction.ConfirmLogoutClick) }
        },
        onFingerprintLearnMore = remember(viewModel) {
            { viewModel.trySendAction(AccountSecurityAction.FingerPrintLearnMoreClick) }
        },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.account_security),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.BackClick) }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            AnimatedVisibility(
                visible = state.shouldShowUnlockActionCard,
                label = "UnlockActionCard",
                exit = actionCardExitAnimation(),
            ) {
                BitwardenActionCard(
                    cardTitle = stringResource(id = BitwardenString.set_up_unlock),
                    actionText = stringResource(BitwardenString.get_started),
                    onActionClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(AccountSecurityAction.UnlockActionCardCtaClick)
                        }
                    },
                    onDismissClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(AccountSecurityAction.UnlockActionCardDismiss)
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

            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.approve_login_requests),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextRow(
                text = stringResource(id = BitwardenString.pending_log_in_requests),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick) }
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("PendingLogInRequestsLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            val biometricSupportStatus = biometricsManager.biometricSupportStatus
            if (biometricSupportStatus != BiometricSupportStatus.NOT_SUPPORTED ||
                !state.removeUnlockWithPinPolicyEnabled ||
                state.isUnlockWithPinEnabled
            ) {
                Spacer(Modifier.height(16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.unlock_options),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            BitwardenUnlockWithBiometricsSwitch(
                biometricSupportStatus = biometricSupportStatus,
                isChecked = state.isUnlockWithBiometricsEnabled || showBiometricsPrompt,
                onDisableBiometrics = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            AccountSecurityAction.UnlockWithBiometricToggleDisabled,
                        )
                    }
                },
                onEnableBiometrics = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.EnableBiometricsClick) }
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("UnlockWithBiometricsSwitch")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            if (!state.removeUnlockWithPinPolicyEnabled || state.isUnlockWithPinEnabled) {
                Spacer(modifier = Modifier.height(height = 8.dp))
                BitwardenUnlockWithPinSwitch(
                    isUnlockWithPasswordEnabled = state.isUnlockWithPasswordEnabled,
                    isUnlockWithPinEnabled = state.isUnlockWithPinEnabled,
                    onUnlockWithPinToggleAction = remember(viewModel) {
                        { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle(it)) }
                    },
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .testTag("UnlockWithPinSwitch")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (state.shouldShowEnableAuthenticatorSync) {
                SyncWithAuthenticatorRow(
                    isChecked = state.isAuthenticatorSyncChecked,
                    onCheckedChange = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                AccountSecurityAction.AuthenticatorSyncToggle(enabled = it),
                            )
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
            }
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.session_timeout),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            SessionTimeoutRow(
                vaultTimeoutPolicy = state.vaultTimeoutPolicy,
                selectedVaultTimeoutType = state.vaultTimeout.type,
                onVaultTimeoutTypeSelect = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.VaultTimeoutTypeSelect(it)) }
                },
                modifier = Modifier
                    .testTag("VaultTimeoutChooser")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            (state.vaultTimeout as? VaultTimeout.Custom)?.let { customTimeout ->
                SessionCustomTimeoutRow(
                    vaultTimeoutPolicy = state.vaultTimeoutPolicy,
                    customVaultTimeout = customTimeout,
                    onCustomVaultTimeoutSelect = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                AccountSecurityAction.CustomVaultTimeoutSelect(it),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
            state.sessionTimeoutSupportText?.let { text ->
                BitwardenSupportingText(
                    text = text(),
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            SessionTimeoutActionRow(
                isEnabled = state.isSessionTimeoutActionEnabled,
                selectedVaultTimeoutAction = state.vaultTimeoutAction,
                onVaultTimeoutActionSelect = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.VaultTimeoutActionSelect(it)) }
                },
                supportingText = state.sessionTimeoutActionSupportingText?.invoke(),
                cardStyle = if (state.sessionTimeoutSupportText == null) {
                    CardStyle.Bottom
                } else {
                    CardStyle.Full
                },
                modifier = Modifier
                    .testTag("VaultTimeoutActionChooser")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.other),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextRow(
                text = stringResource(id = BitwardenString.account_fingerprint_phrase),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.AccountFingerprintPhraseClick) }
                },
                cardStyle = CardStyle.Top(),
                modifier = Modifier
                    .testTag("AccountFingerprintPhraseLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            BitwardenExternalLinkRow(
                text = stringResource(id = BitwardenString.two_step_login),
                onConfirmClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick) }
                },
                withDivider = false,
                dialogTitle = stringResource(id = BitwardenString.continue_to_web_app),
                dialogMessage = stringResource(
                    id = BitwardenString.two_step_login_description_long,
                ),
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("TwoStepLoginLinkItemView")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            if (state.isUnlockWithPasswordEnabled) {
                BitwardenExternalLinkRow(
                    text = stringResource(id = BitwardenString.change_master_password),
                    onConfirmClick = remember(viewModel) {
                        { viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick) }
                    },
                    withDivider = false,
                    dialogTitle = stringResource(id = BitwardenString.continue_to_web_app),
                    dialogMessage = stringResource(
                        id = BitwardenString.change_master_password_description_long,
                    ),
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            if (state.hasUnlockMechanism) {
                BitwardenTextRow(
                    text = stringResource(id = BitwardenString.lock_now),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(AccountSecurityAction.LockNowClick) }
                    },
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .testTag("LockNowLabel")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            BitwardenTextRow(
                text = stringResource(id = BitwardenString.log_out),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("LogOutLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            BitwardenTextRow(
                text = stringResource(id = BitwardenString.delete_account),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.DeleteAccountClick) }
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag("DeleteAccountLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AccountSecurityDialogs(
    state: AccountSecurityState,
    onDismissRequest: () -> Unit,
    onConfirmLogoutClick: () -> Unit,
    onFingerprintLearnMore: () -> Unit,
) {
    when (val dialogState = state.dialog) {
        AccountSecurityDialog.ConfirmLogout -> BitwardenLogoutConfirmationDialog(
            onDismissRequest = onDismissRequest,
            onConfirmClick = onConfirmLogoutClick,
        )

        AccountSecurityDialog.FingerprintPhrase -> FingerPrintPhraseDialog(
            fingerprintPhrase = state.fingerprintPhrase,
            onDismissRequest = onDismissRequest,
            onLearnMore = onFingerprintLearnMore,
        )

        is AccountSecurityDialog.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        is AccountSecurityDialog.Error -> BitwardenBasicDialog(
            title = dialogState.title(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
        )

        null -> Unit
    }
}

@Composable
private fun SessionTimeoutRow(
    vaultTimeoutPolicy: VaultTimeoutPolicy?,
    selectedVaultTimeoutType: VaultTimeout.Type,
    onVaultTimeoutTypeSelect: (VaultTimeout.Type) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalResources.current,
) {
    var shouldShowNeverTimeoutConfirmationDialog by remember { mutableStateOf(false) }
    val vaultTimeoutOptions = rememberSessionTimeoutOptions(vaultTimeoutPolicy)
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.session_timeout),
        options = vaultTimeoutOptions.map { it.displayLabel() }.toImmutableList(),
        selectedOption = selectedVaultTimeoutType.displayLabel(),
        onOptionSelected = { selectedType ->
            val selectedOption = vaultTimeoutOptions.first {
                it.displayLabel.toString(resources) == selectedType
            }
            if (selectedOption == VaultTimeout.Type.NEVER) {
                shouldShowNeverTimeoutConfirmationDialog = true
            } else {
                onVaultTimeoutTypeSelect(selectedOption)
            }
        },
        isEnabled = vaultTimeoutOptions.size > 1,
        textFieldTestTag = "SessionTimeoutStatusLabel",
        cardStyle = CardStyle.Top(),
        modifier = modifier,
    )

    if (shouldShowNeverTimeoutConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.warning),
            message = stringResource(id = BitwardenString.never_lock_warning),
            confirmButtonText = stringResource(id = BitwardenString.okay),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowNeverTimeoutConfirmationDialog = false
                onVaultTimeoutTypeSelect(VaultTimeout.Type.NEVER)
            },
            onDismissClick = { shouldShowNeverTimeoutConfirmationDialog = false },
            onDismissRequest = { shouldShowNeverTimeoutConfirmationDialog = false },
        )
    }
}

@Composable
private fun SessionCustomTimeoutRow(
    vaultTimeoutPolicy: VaultTimeoutPolicy?,
    customVaultTimeout: VaultTimeout.Custom,
    onCustomVaultTimeoutSelect: (VaultTimeout.Custom) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowViolatesPoliciesDialog by remember { mutableStateOf(false) }
    BitwardenTimePickerButton(
        label = stringResource(id = BitwardenString.custom_timeout),
        totalMinutes = customVaultTimeout.vaultTimeoutInMinutes,
        onTimeSelect = { minutes ->
            if (vaultTimeoutPolicy?.minutes != null && minutes > vaultTimeoutPolicy.minutes) {
                shouldShowViolatesPoliciesDialog = true
            } else {
                onCustomVaultTimeoutSelect(VaultTimeout.Custom(minutes))
            }
        },
        is24Hour = true,
        supportingContent = null,
        cardStyle = CardStyle.Middle(),
        modifier = modifier,
    )

    if (shouldShowViolatesPoliciesDialog) {
        BitwardenBasicDialog(
            title = stringResource(id = BitwardenString.warning),
            message = stringResource(id = BitwardenString.vault_timeout_to_large),
            onDismissRequest = {
                shouldShowViolatesPoliciesDialog = false
                vaultTimeoutPolicy?.minutes?.let {
                    onCustomVaultTimeoutSelect(VaultTimeout.Custom(it))
                }
            },
        )
    }
}

@Composable
private fun SessionTimeoutActionRow(
    isEnabled: Boolean,
    selectedVaultTimeoutAction: VaultTimeoutAction,
    onVaultTimeoutActionSelect: (VaultTimeoutAction) -> Unit,
    supportingText: String?,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    resources: Resources = LocalResources.current,
) {
    var shouldShowLogoutActionConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenMultiSelectButton(
        isEnabled = isEnabled,
        label = stringResource(id = BitwardenString.session_timeout_action),
        options = VaultTimeoutAction.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = selectedVaultTimeoutAction.displayLabel(),
        onOptionSelected = { action ->
            val selectedAction = VaultTimeoutAction.entries.first {
                it.displayLabel.toString(resources) == action
            }
            if (selectedAction == VaultTimeoutAction.LOGOUT) {
                shouldShowLogoutActionConfirmationDialog = true
            } else {
                onVaultTimeoutActionSelect(selectedAction)
            }
        },
        supportingText = supportingText,
        textFieldTestTag = "SessionTimeoutActionStatusLabel",
        cardStyle = cardStyle,
        modifier = modifier,
    )

    if (shouldShowLogoutActionConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.vault_timeout_log_out_confirmation_title),
            message = stringResource(
                id = BitwardenString.vault_timeout_log_out_confirmation_message,
            ),
            confirmButtonText = stringResource(id = BitwardenString.yes),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowLogoutActionConfirmationDialog = false
                onVaultTimeoutActionSelect(VaultTimeoutAction.LOGOUT)
            },
            onDismissClick = {
                shouldShowLogoutActionConfirmationDialog = false
            },
            onDismissRequest = {
                shouldShowLogoutActionConfirmationDialog = false
            },
        )
    }
}

@Composable
private fun ColumnScope.SyncWithAuthenticatorRow(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BitwardenListHeaderText(
        label = stringResource(BitwardenString.authenticator_sync),
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    BitwardenSwitch(
        label = stringResource(BitwardenString.allow_bitwarden_authenticator_syncing),
        onCheckedChange = onCheckedChange,
        isChecked = isChecked,
        cardStyle = CardStyle.Full,
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
}

@Composable
private fun FingerPrintPhraseDialog(
    fingerprintPhrase: Text,
    onDismissRequest: () -> Unit,
    onLearnMore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.close),
                onClick = onDismissRequest,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.learn_more),
                onClick = onLearnMore,
            )
        },
        title = {
            Text(
                text = stringResource(id = BitwardenString.fingerprint_phrase),
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    text = "${stringResource(id = BitwardenString.your_accounts_fingerprint)}:",
                    color = BitwardenTheme.colorScheme.text.primary,
                    style = BitwardenTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = fingerprintPhrase(),
                    color = BitwardenTheme.colorScheme.text.codePink,
                    style = BitwardenTheme.typography.sensitiveInfoSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        containerColor = BitwardenTheme.colorScheme.background.primary,
        iconContentColor = BitwardenTheme.colorScheme.icon.secondary,
        titleContentColor = BitwardenTheme.colorScheme.text.primary,
        textContentColor = BitwardenTheme.colorScheme.text.primary,
    )
}

@Composable
private fun rememberSessionTimeoutOptions(
    vaultTimeoutPolicy: VaultTimeoutPolicy?,
): ImmutableList<VaultTimeout.Type> = remember(vaultTimeoutPolicy) {
    VaultTimeout.Type
        .entries
        .filter { timeoutType ->
            when (vaultTimeoutPolicy?.type) {
                PolicyInformation.VaultTimeout.Type.NEVER -> {
                    // We allow everything here.
                    true
                }

                PolicyInformation.VaultTimeout.Type.ON_APP_RESTART,
                PolicyInformation.VaultTimeout.Type.ON_SYSTEM_LOCK,
                    -> {
                    // We allow everything but never here.
                    timeoutType != VaultTimeout.Type.NEVER
                }

                PolicyInformation.VaultTimeout.Type.IMMEDIATELY -> {
                    // Only allow immediately, everything else is blocked.
                    timeoutType == VaultTimeout.Type.IMMEDIATELY
                }

                PolicyInformation.VaultTimeout.Type.CUSTOM -> {
                    // Filter out all values above the specified amount. Custom set timeouts
                    // that exceed the max value will be constrained in the VM.
                    timeoutType.minutes <= (vaultTimeoutPolicy.minutes ?: Int.MAX_VALUE)
                }

                null -> {
                    // If the type is null, there could still be a policy in place from a
                    // legacy server that is not sending a type. So we still filter as though
                    // it were a custom type in that scenario, otherwise we allow everything.
                    vaultTimeoutPolicy
                        ?.minutes
                        ?.let { minutes -> timeoutType.minutes <= minutes }
                        ?: true
                }
            }
        }
        .toImmutableList()
}
