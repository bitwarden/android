package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLogoutConfirmationDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTimePickerDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenPolicyWarningText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialColors
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography
import com.x8bit.bitwarden.ui.platform.theme.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import com.x8bit.bitwarden.ui.platform.util.minutes
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import java.time.LocalTime

private const val MINUTES_PER_HOUR = 60

/**
 * Displays the account security screen.
 */
@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val resources = context.resources
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

            is AccountSecurityEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }
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
                navigationIcon = painterResource(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.BackClick) }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.approve_login_requests),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            ApprovePasswordlessLoginsRow(
                isApproveLoginRequestsEnabled = state.isApproveLoginRequestsEnabled,
                onApprovePasswordlessLoginsAction = remember(viewModel) {
                    { viewModel.trySendAction(it) }
                },
                permissionsManager = permissionsManager,
                onPushNotificationConfirm = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.PushNotificationConfirm) }
                },
                modifier = Modifier
                    .semantics { testTag = "ApproveLoginRequestsSwitch" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            if (state.isApproveLoginRequestsEnabled) {
                BitwardenTextRow(
                    text = stringResource(id = R.string.pending_log_in_requests),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick) }
                    },
                    modifier = Modifier
                        .semantics { testTag = "PendingLogInRequestsLabel" }
                        .fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.unlock_options),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            UnlockWithBiometricsRow(
                isChecked = state.isUnlockWithBiometricsEnabled,
                onBiometricToggle = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(it)) }
                },
                biometricsManager = biometricsManager,
                modifier = Modifier
                    .semantics { testTag = "UnlockWithBiometricsSwitch" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            UnlockWithPinRow(
                isUnlockWithPinEnabled = state.isUnlockWithPinEnabled,
                onUnlockWithPinToggleAction = remember(viewModel) {
                    { viewModel.trySendAction(it) }
                },
                modifier = Modifier
                    .semantics { testTag = "UnlockWithPinSwitch" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.session_timeout),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            SessionTimeoutPolicyRow(
                vaultTimeoutPolicyMinutes = state.vaultTimeoutPolicyMinutes,
                vaultTimeoutPolicyAction = state.vaultTimeoutPolicyAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            SessionTimeoutRow(
                vaultTimeoutPolicyMinutes = state.vaultTimeoutPolicyMinutes,
                selectedVaultTimeoutType = state.vaultTimeout.type,
                onVaultTimeoutTypeSelect = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.VaultTimeoutTypeSelect(it)) }
                },
                modifier = Modifier
                    .semantics { testTag = "VaultTimeoutChooser" }
                    .fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SessionTimeoutActionRow(
                vaultTimeoutPolicyAction = state.vaultTimeoutPolicyAction,
                selectedVaultTimeoutAction = state.vaultTimeoutAction,
                onVaultTimeoutActionSelect = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.VaultTimeoutActionSelect(it)) }
                },
                modifier = Modifier
                    .semantics { testTag = "VaultTimeoutActionChooser" }
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.other),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenTextRow(
                text = stringResource(id = R.string.account_fingerprint_phrase),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.AccountFingerprintPhraseClick) }
                },
                modifier = Modifier
                    .semantics { testTag = "AccountFingerprintPhraseLabel" }
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
                modifier = Modifier
                    .semantics { testTag = "TwoStepLoginLinkItemView" }
                    .fillMaxWidth(),
            )
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
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenTextRow(
                text = stringResource(id = R.string.lock_now),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.LockNowClick) }
                },
                modifier = Modifier
                    .semantics { testTag = "LockNowLabel" }
                    .fillMaxWidth(),
            )
            BitwardenTextRow(
                text = stringResource(id = R.string.log_out),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
                },
                modifier = Modifier
                    .semantics { testTag = "LogOutLabel" }
                    .fillMaxWidth(),
            )
            BitwardenTextRow(
                text = stringResource(id = R.string.delete_account),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AccountSecurityAction.DeleteAccountClick) }
                },
                modifier = Modifier
                    .semantics { testTag = "DeleteAccountLabel" }
                    .fillMaxWidth(),
            )
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
            visibilityState = LoadingDialogState.Shown(text = dialogState.message),
        )

        null -> Unit
    }
}

@Composable
private fun UnlockWithBiometricsRow(
    isChecked: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    biometricsManager: BiometricsManager,
    modifier: Modifier = Modifier,
) {
    if (!biometricsManager.isBiometricsSupported) return
    var showBiometricsPrompt by rememberSaveable { mutableStateOf(false) }
    BitwardenWideSwitch(
        modifier = modifier,
        label = stringResource(
            id = R.string.unlock_with,
            stringResource(id = R.string.biometrics),
        ),
        isChecked = isChecked || showBiometricsPrompt,
        onCheckedChange = { toggled ->
            if (toggled) {
                showBiometricsPrompt = true
                biometricsManager.promptBiometrics(
                    onSuccess = {
                        onBiometricToggle(true)
                        showBiometricsPrompt = false
                    },
                    onCancel = { showBiometricsPrompt = false },
                    onLockOut = { showBiometricsPrompt = false },
                    onError = { showBiometricsPrompt = false },
                )
            } else {
                onBiometricToggle(false)
            }
        },
    )
}

@Suppress("LongMethod")
@Composable
private fun UnlockWithPinRow(
    isUnlockWithPinEnabled: Boolean,
    onUnlockWithPinToggleAction: (AccountSecurityAction.UnlockWithPinToggle) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowPinInputDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowPinConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    BitwardenWideSwitch(
        label = stringResource(id = R.string.unlock_with_pin),
        isChecked = isUnlockWithPinEnabled,
        onCheckedChange = { isChecked ->
            if (isChecked) {
                onUnlockWithPinToggleAction(
                    AccountSecurityAction.UnlockWithPinToggle.PendingEnabled,
                )
                shouldShowPinInputDialog = true
            } else {
                onUnlockWithPinToggleAction(
                    AccountSecurityAction.UnlockWithPinToggle.Disabled,
                )
            }
        },
        modifier = modifier,
    )

    when {
        shouldShowPinInputDialog -> {
            PinInputDialog(
                pin = pin,
                onPinChange = { pin = it },
                onCancelClick = {
                    shouldShowPinInputDialog = false
                    onUnlockWithPinToggleAction(
                        AccountSecurityAction.UnlockWithPinToggle.Disabled,
                    )
                    pin = ""
                },
                onSubmitClick = {
                    if (pin.isNotEmpty()) {
                        shouldShowPinInputDialog = false
                        shouldShowPinConfirmationDialog = true
                        onUnlockWithPinToggleAction(
                            AccountSecurityAction.UnlockWithPinToggle.PendingEnabled,
                        )
                    } else {
                        shouldShowPinInputDialog = false
                        onUnlockWithPinToggleAction(
                            AccountSecurityAction.UnlockWithPinToggle.Disabled,
                        )
                    }
                },
                onDismissRequest = {
                    shouldShowPinInputDialog = false
                    onUnlockWithPinToggleAction(
                        AccountSecurityAction.UnlockWithPinToggle.Disabled,
                    )
                    pin = ""
                },
            )
        }

        shouldShowPinConfirmationDialog -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.unlock_with_pin),
                message = stringResource(id = R.string.pin_require_master_password_restart),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.no),
                onConfirmClick = {
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        AccountSecurityAction.UnlockWithPinToggle.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = true,
                        ),
                    )
                    pin = ""
                },
                onDismissClick = {
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        AccountSecurityAction.UnlockWithPinToggle.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = false,
                        ),
                    )
                    pin = ""
                },
                onDismissRequest = {
                    // Dismissing the dialog is the same as requiring a master password
                    // confirmation.
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        AccountSecurityAction.UnlockWithPinToggle.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = true,
                        ),
                    )
                    pin = ""
                },
            )
        }
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

        BitwardenPolicyWarningText(
            text = policyText(),
            modifier = modifier,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SessionTimeoutRow(
    vaultTimeoutPolicyMinutes: Int?,
    selectedVaultTimeoutType: VaultTimeout.Type,
    onVaultTimeoutTypeSelect: (VaultTimeout.Type) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowSelectionDialog by remember { mutableStateOf(false) }
    var shouldShowNeverTimeoutConfirmationDialog by remember { mutableStateOf(false) }
    BitwardenTextRow(
        text = stringResource(id = R.string.session_timeout),
        onClick = { shouldShowSelectionDialog = true },
        modifier = modifier,
    ) {
        Text(
            text = selectedVaultTimeoutType.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { testTag = "SessionTimeoutStatusLabel" },
        )
    }

    when {
        shouldShowSelectionDialog -> {
            val vaultTimeoutOptions = VaultTimeout.Type.entries
                .filter {
                    it.minutes <= (vaultTimeoutPolicyMinutes ?: Int.MAX_VALUE)
                }

            BitwardenSelectionDialog(
                title = stringResource(id = R.string.session_timeout),
                onDismissRequest = { shouldShowSelectionDialog = false },
            ) {
                vaultTimeoutOptions.forEach { vaultTimeoutOption ->
                    BitwardenSelectionRow(
                        text = vaultTimeoutOption.displayLabel,
                        onClick = {
                            shouldShowSelectionDialog = false
                            val selectedType =
                                vaultTimeoutOptions.first { it == vaultTimeoutOption }
                            if (selectedType == VaultTimeout.Type.NEVER) {
                                shouldShowNeverTimeoutConfirmationDialog = true
                            } else {
                                onVaultTimeoutTypeSelect(selectedType)
                            }
                        },
                        isSelected = selectedVaultTimeoutType == vaultTimeoutOption,
                    )
                }
            }
        }

        shouldShowNeverTimeoutConfirmationDialog -> {
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
        modifier = modifier,
    ) {
        val formattedTime = LocalTime
            .ofSecondOfDay(
                vaultTimeoutInMinutes * MINUTES_PER_HOUR.toLong(),
            )
            .toFormattedPattern("HH:mm")
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            visibilityState = BasicDialogState.Shown(
                title = R.string.warning.asText(),
                message = R.string.vault_timeout_to_large.asText(),
            ),
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

@Suppress("LongMethod")
@Composable
private fun SessionTimeoutActionRow(
    vaultTimeoutPolicyAction: String?,
    selectedVaultTimeoutAction: VaultTimeoutAction,
    onVaultTimeoutActionSelect: (VaultTimeoutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowSelectionDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowLogoutActionConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenTextRow(
        text = stringResource(id = R.string.session_timeout_action),
        onClick = {
            // The option is not selectable if there's a policy in place.
            if (vaultTimeoutPolicyAction != null) return@BitwardenTextRow
            shouldShowSelectionDialog = true
        },
        modifier = modifier,
    ) {
        Text(
            text = selectedVaultTimeoutAction.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { testTag = "SessionTimeoutActionStatusLabel" },
        )
    }
    when {
        shouldShowSelectionDialog -> {
            BitwardenSelectionDialog(
                title = stringResource(id = R.string.vault_timeout_action),
                onDismissRequest = { shouldShowSelectionDialog = false },
            ) {
                val vaultTimeoutActionOptions = VaultTimeoutAction.entries
                vaultTimeoutActionOptions.forEach { option ->
                    BitwardenSelectionRow(
                        text = option.displayLabel,
                        isSelected = option == selectedVaultTimeoutAction,
                        onClick = {
                            shouldShowSelectionDialog = false
                            val seletedAction = vaultTimeoutActionOptions.first { it == option }
                            if (seletedAction == VaultTimeoutAction.LOGOUT) {
                                shouldShowLogoutActionConfirmationDialog = true
                            } else {
                                onVaultTimeoutActionSelect(seletedAction)
                            }
                        },
                    )
                }
            }
        }

        shouldShowLogoutActionConfirmationDialog -> {
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
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column {
                Text(
                    text = "${stringResource(id = R.string.your_accounts_fingerprint)}:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = fingerprintPhrase(),
                    color = LocalNonMaterialColors.current.fingerprint,
                    style = LocalNonMaterialTypography.current.sensitiveInfoSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Suppress("LongMethod")
@Composable
private fun ApprovePasswordlessLoginsRow(
    isApproveLoginRequestsEnabled: Boolean,
    @Suppress("MaxLineLength")
    onApprovePasswordlessLoginsAction: (AccountSecurityAction.ApprovePasswordlessLoginsToggle) -> Unit,
    onPushNotificationConfirm: () -> Unit,
    permissionsManager: PermissionsManager,
    modifier: Modifier = Modifier,
) {
    var shouldShowConfirmationDialog by remember { mutableStateOf(false) }
    var shouldShowPermissionDialog by remember { mutableStateOf(false) }
    BitwardenWideSwitch(
        label = stringResource(
            id = R.string.use_this_device_to_approve_login_requests_made_from_other_devices,
        ),
        isChecked = isApproveLoginRequestsEnabled,
        onCheckedChange = { isChecked ->
            if (isChecked) {
                onApprovePasswordlessLoginsAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.PendingEnabled,
                )
                shouldShowConfirmationDialog = true
            } else {
                onApprovePasswordlessLoginsAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
                )
            }
        },
        modifier = modifier,
    )

    if (shouldShowConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.approve_login_requests),
            message = stringResource(
                id = R.string.use_this_device_to_approve_login_requests_made_from_other_devices,
            ),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.no),
            onConfirmClick = {
                onApprovePasswordlessLoginsAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled,
                )
                shouldShowConfirmationDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("MaxLineLength")
                    if (!permissionsManager.checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                        shouldShowPermissionDialog = true
                    }
                }
            },
            onDismissClick = {
                onApprovePasswordlessLoginsAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
                )
                shouldShowConfirmationDialog = false
            },
            onDismissRequest = {
                onApprovePasswordlessLoginsAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
                )
                shouldShowConfirmationDialog = false
            },
        )
    }

    if (shouldShowPermissionDialog) {
        BitwardenTwoButtonDialog(
            title = null,
            message = stringResource(
                id = R.string.receive_push_notifications_for_new_login_requests,
            ),
            confirmButtonText = stringResource(id = R.string.settings),
            dismissButtonText = stringResource(id = R.string.no_thanks),
            onConfirmClick = {
                shouldShowPermissionDialog = false
                onPushNotificationConfirm()
            },
            onDismissClick = {
                shouldShowPermissionDialog = false
            },
            onDismissRequest = {
                shouldShowPermissionDialog = false
            },
        )
    }
}
