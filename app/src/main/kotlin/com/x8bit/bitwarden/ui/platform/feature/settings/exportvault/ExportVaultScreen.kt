package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthIndicator
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Export Vault screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ExportVaultScreen(
    onNavigateBack: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: ExportVaultViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val exportVaultLocationReceived: (Uri) -> Unit = remember {
        { viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(it)) }
    }
    val fileSaverLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            exportVaultLocationReceived.invoke(it.uri)
        }
    }
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExportVaultEvent.NavigateBack -> onNavigateBack()
            is ExportVaultEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            is ExportVaultEvent.NavigateToSelectExportDataLocation -> {
                fileSaverLauncher.launch(
                    intentManager.createDocumentIntent(
                        fileName = event.fileName,
                    ),
                )
            }
        }
    }

    var shouldShowConfirmationDialog by remember { mutableStateOf(false) }
    val confirmExportVaultClicked = remember(viewModel) {
        { viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked) }
    }
    if (shouldShowConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.export_vault_confirmation_title),
            message = if (state.exportFormat == ExportVaultFormat.JSON_ENCRYPTED) {
                stringResource(id = BitwardenString.export_vault_file_pw_protect_info)
            } else {
                stringResource(
                    id = BitwardenString.export_vault_warning,
                )
            },
            confirmButtonText = stringResource(id = BitwardenString.export_vault),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowConfirmationDialog = false
                confirmExportVaultClicked()
            },
            onDismissClick = { shouldShowConfirmationDialog = false },
            onDismissRequest = { shouldShowConfirmationDialog = false },
        )
    }

    when (val dialog = state.dialogState) {
        is ExportVaultState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                throwable = dialog.error,
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ExportVaultAction.DialogDismiss) }
                },
            )
        }

        is ExportVaultState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.export_vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ExportVaultAction.CloseButtonClick) }
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        ExportVaultScreenContent(
            state = state,
            onConfirmFilePasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.ConfirmFilePasswordInputChange(it)) }
            },
            onExportFormatOptionSelected = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.ExportFormatOptionSelect(it)) }
            },
            onFilePasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.FilePasswordInputChange(it)) }
            },
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(it)) }
            },
            onSendCodeClicked = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.SendCodeClick) }
            },
            onExportVaultClick = { shouldShowConfirmationDialog = true },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun ExportVaultScreenContent(
    state: ExportVaultState,
    onConfirmFilePasswordInputChanged: (String) -> Unit,
    onExportFormatOptionSelected: (ExportVaultFormat) -> Unit,
    onFilePasswordInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onSendCodeClicked: () -> Unit,
    onExportVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        if (state.policyPreventsExport) {
            BitwardenInfoCalloutCard(
                text = stringResource(
                    id = BitwardenString.disable_personal_vault_export_policy_in_effect,
                ),
                modifier = Modifier
                    .testTag("DisablePrivateVaultPolicyLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        val resources = LocalResources.current
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.file_format),
            options = ExportVaultFormat.entries.map { it.displayLabel() }.toImmutableList(),
            selectedOption = state.exportFormat.displayLabel(),
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ExportVaultFormat
                    .entries
                    .first { it.displayLabel(resources) == selectedOptionLabel }
                onExportFormatOptionSelected(selectedOption)
            },
            isEnabled = !state.policyPreventsExport,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("FileFormatPicker")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        if (state.exportFormat == ExportVaultFormat.JSON_ENCRYPTED) {
            Spacer(modifier = Modifier.height(8.dp))
            var showPassword by rememberSaveable { mutableStateOf(false) }
            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.file_password),
                value = state.filePasswordInput,
                onValueChange = onFilePasswordInputChanged,
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                supportingContent = {
                    PasswordStrengthIndicator(
                        state = state.passwordStrengthState,
                        currentCharacterCount = state.passwordInput.length,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(id = BitwardenString.password_used_to_export),
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                showPasswordTestTag = "FilePasswordEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.confirm_file_password),
                value = state.confirmFilePasswordInput,
                onValueChange = onConfirmFilePasswordInputChanged,
                showPassword = showPassword,
                showPasswordChange = { showPassword = it },
                passwordFieldTestTag = "ConfirmFilePasswordEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        if (state.showSendCodeButton) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = BitwardenString.send_verification_code_to_email),
                textAlign = TextAlign.Start,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenOutlinedButton(
                label = stringResource(BitwardenString.send_code),
                onClick = onSendCodeClicked,
                isEnabled = !state.policyPreventsExport,
                modifier = Modifier
                    .testTag("SendTOTPCodeButton")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.verification_code),
                value = state.passwordInput,
                readOnly = state.policyPreventsExport,
                supportingText = stringResource(id = BitwardenString.confirm_your_identity),
                onValueChange = onPasswordInputChanged,
                keyboardType = KeyboardType.Number,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        } else {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.master_password),
                supportingText = stringResource(
                    id = BitwardenString.export_vault_master_password_description,
                ),
                value = state.passwordInput,
                readOnly = state.policyPreventsExport,
                onValueChange = onPasswordInputChanged,
                passwordFieldTestTag = "MasterPasswordEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.export_vault),
            onClick = onExportVaultClick,
            isEnabled = !state.policyPreventsExport,
            modifier = Modifier
                .testTag("ExportVaultButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
    }
}
