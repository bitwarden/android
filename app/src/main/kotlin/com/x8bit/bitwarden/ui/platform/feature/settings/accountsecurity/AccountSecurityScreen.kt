package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.content.res.Resources
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLogoutConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTimePickerDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithBiometricsSwitch
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenUnlockWithPinSwitch
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricSupportStatus
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import com.x8bit.bitwarden.ui.platform.util.minutes
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalTime
import javax.crypto.Cipher

private const val MINUTES_PER_HOUR = 60

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
    val context = LocalContext.current
    val resources = context.resources
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
                intentManager.startApplicationDetailsSettingsActivity()
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

            is AccountSecurityEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
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
                title = stringResource(id = R.string.account_security),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
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
                    cardTitle = stringResource(id = R.string.set_up_unlock),
                    actionText = stringResource(R.string.get_started),
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
                label = stringResource(id = R.string.approve_login_requests),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextRow(
                text = stringResource(id = R.string.pending_log_in_requests),
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
                    label = stringResource(id = R.string.unlock_options),
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
                label = stringResource(id = R.string.session_timeout),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            SessionTimeoutPolicyRow(
                vaultTimeoutPolicyMinutes = state.vaultTimeoutPolicyMinutes,
                vaultTimeoutPolicyAction = state.vaultTimeoutPolicyAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            SessionTimeoutRow(
                vaultTimeoutPolicyMinutes = state.vaultTimeoutPolicyMinutes,
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
                    vaultTimeoutPolicyMinutes = state.vaultTimeoutPolicyMinutes,
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
            SessionTimeoutActionRow(
                isEnabled = state.hasUnlockMechanism,
                vaultTimeoutPolicyAction = state.vaultTimeoutPolicyAction,
                selectedVaultTimeoutAction = state.vaultTimeoutAction,
                onVaultTimeoutActionSelect = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.VaultTimeoutActionSelect(it)) }
                },
                modifier = Modifier
                    .testTag("VaultTimeoutActionChooser")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.other),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenTextRow(
                text = stringResource(id = R.string.account_fingerprint_phrase),
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
                text = stringResource(id = R.string.two_step_login),
                onConfirmClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick) }
                },
                withDivider = false,
                dialogTitle = stringResource(id = R.string.continue_to_web_app),
                dialogMessage = stringResource(id = R.string.two_step_login_description_long),
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("TwoStepLoginLinkItemView")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            if (state.isUnlockWithPasswordEnabled) {
                BitwardenExternalLinkRow(
                    text = stringResource(id = R.string.change_master_password),
                    onConfirmClick = remember(viewModel) {
                        { viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick) }
                    },
                    withDivider = false,
                    dialogTitle = stringResource(id = R.string.continue_to_web_app),
                    dialogMessage = stringResource(
                        id = R.string.change_master_password_description_long,
                    ),
                    cardStyle = CardStyle.Middle(),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            if (state.hasUnlockMechanism) {
                BitwardenTextRow(
                    text = stringResource(id = R.string.lock_now),
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
                text = stringResource(id = R.string.log_out),
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
                text = stringResource(id = R.string.delete_account),
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
private fun SessionTimeoutPolicyRow(
    vaultTimeoutPolicyMinutes: Int?,
    vaultTimeoutPolicyAction: String?,
    modifier: Modifier = Modifier,
) {
    // Show the policy warning if applicable.
    if (vaultTimeoutPolicyMinutes != null || !vaultTimeoutPolicyAction.isNullOrBlank()) {
        // Calculate the hours and minutes to show in the policy label.
        val hours = vaultTimeoutPolicyMinutes?.floorDiv(MINUTES_PER_HOUR)
        val minutes = vaultTimeoutPolicyMinutes?.mod(MINUTES_PER_HOUR)

        // Get the localized version of the action.
        val action = if (vaultTimeoutPolicyAction == "lock") {
            R.string.lock.asText()
        } else {
            R.string.log_out.asText()
        }

        val policyText = if (hours == null || minutes == null) {
            R.string.vault_timeout_action_policy_in_effect.asText(action)
        } else if (vaultTimeoutPolicyAction.isNullOrBlank()) {
            R.string.vault_timeout_policy_in_effect.asText(hours, minutes)
        } else {
            R.string.vault_timeout_policy_with_action_in_effect.asText(hours, minutes, action)
        }

        BitwardenInfoCalloutCard(
            text = policyText(),
            modifier = modifier,
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
    }
}

@Composable
private fun SessionTimeoutRow(
    vaultTimeoutPolicyMinutes: Int?,
    selectedVaultTimeoutType: VaultTimeout.Type,
    onVaultTimeoutTypeSelect: (VaultTimeout.Type) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    var shouldShowNeverTimeoutConfirmationDialog by remember { mutableStateOf(false) }
    val vaultTimeoutOptions = VaultTimeout.Type
        .entries
        .filter { it.minutes <= (vaultTimeoutPolicyMinutes ?: Int.MAX_VALUE) }
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.session_timeout),
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
        textFieldTestTag = "SessionTimeoutStatusLabel",
        cardStyle = CardStyle.Top(),
        modifier = modifier,
    )

    if (shouldShowNeverTimeoutConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.warning),
            message = stringResource(id = R.string.never_lock_warning),
            confirmButtonText = stringResource(id = R.string.ok),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                shouldShowNeverTimeoutConfirmationDialog = false
                onVaultTimeoutTypeSelect(VaultTimeout.Type.NEVER)
            },
            onDismissClick = { shouldShowNeverTimeoutConfirmationDialog = false },
            onDismissRequest = { shouldShowNeverTimeoutConfirmationDialog = false },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SessionCustomTimeoutRow(
    vaultTimeoutPolicyMinutes: Int?,
    customVaultTimeout: VaultTimeout.Custom,
    onCustomVaultTimeoutSelect: (VaultTimeout.Custom) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowViolatesPoliciesDialog by remember { mutableStateOf(false) }
    val vaultTimeoutInMinutes = customVaultTimeout.vaultTimeoutInMinutes
    BitwardenTextRow(
        text = stringResource(id = R.string.custom),
        onClick = { shouldShowTimePickerDialog = true },
        cardStyle = CardStyle.Middle(),
        modifier = modifier,
    ) {
        val formattedTime = LocalTime
            .ofSecondOfDay(
                vaultTimeoutInMinutes * MINUTES_PER_HOUR.toLong(),
            )
            .toFormattedPattern("HH:mm")
        Text(
            text = formattedTime,
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }

    if (shouldShowTimePickerDialog) {
        BitwardenTimePickerDialog(
            initialHour = vaultTimeoutInMinutes / MINUTES_PER_HOUR,
            initialMinute = vaultTimeoutInMinutes.mod(MINUTES_PER_HOUR),
            onTimeSelect = { hour, minute ->
                shouldShowTimePickerDialog = false

                val totalMinutes = (hour * MINUTES_PER_HOUR) + minute
                if (vaultTimeoutPolicyMinutes != null &&
                    totalMinutes > vaultTimeoutPolicyMinutes
                ) {
                    shouldShowViolatesPoliciesDialog = true
                } else {
                    onCustomVaultTimeoutSelect(
                        VaultTimeout.Custom(
                            vaultTimeoutInMinutes = totalMinutes,
                        ),
                    )
                }
            },
            onDismissRequest = { shouldShowTimePickerDialog = false },
            is24Hour = true,
        )
    }

    if (shouldShowViolatesPoliciesDialog) {
        BitwardenBasicDialog(
            title = stringResource(id = R.string.warning),
            message = stringResource(id = R.string.vault_timeout_to_large),
            onDismissRequest = {
                shouldShowViolatesPoliciesDialog = false
                vaultTimeoutPolicyMinutes?.let {
                    onCustomVaultTimeoutSelect(
                        VaultTimeout.Custom(
                            vaultTimeoutInMinutes = it,
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun SessionTimeoutActionRow(
    isEnabled: Boolean,
    vaultTimeoutPolicyAction: String?,
    selectedVaultTimeoutAction: VaultTimeoutAction,
    onVaultTimeoutActionSelect: (VaultTimeoutAction) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    var shouldShowLogoutActionConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenMultiSelectButton(
        isEnabled = isEnabled,
        label = stringResource(id = R.string.session_timeout_action),
        options = VaultTimeoutAction.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = selectedVaultTimeoutAction.displayLabel(),
        onOptionSelected = { action ->
            // The option is not selectable if there's a policy in place.
            if (vaultTimeoutPolicyAction != null) return@BitwardenMultiSelectButton
            val selectedAction = VaultTimeoutAction.entries.first {
                it.displayLabel.toString(resources) == action
            }
            if (selectedAction == VaultTimeoutAction.LOGOUT) {
                shouldShowLogoutActionConfirmationDialog = true
            } else {
                onVaultTimeoutActionSelect(selectedAction)
            }
        },
        supportingText = stringResource(
            id = R.string.set_up_an_unlock_option_to_change_your_vault_timeout_action,
        )
            .takeUnless { isEnabled },
        textFieldTestTag = "SessionTimeoutActionStatusLabel",
        cardStyle = CardStyle.Bottom,
        modifier = modifier,
    )

    if (shouldShowLogoutActionConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.warning),
            message = stringResource(id = R.string.vault_timeout_log_out_confirmation),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
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
        label = stringResource(R.string.authenticator_sync),
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin()
            .padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(height = 8.dp))
    BitwardenSwitch(
        label = stringResource(R.string.allow_bitwarden_authenticator_syncing),
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
                label = stringResource(id = R.string.close),
                onClick = onDismissRequest,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = R.string.learn_more),
                onClick = onLearnMore,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.fingerprint_phrase),
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    text = "${stringResource(id = R.string.your_accounts_fingerprint)}:",
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
