package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenPolicyWarningText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
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
    val context = LocalContext.current

    val exportVaultLocationReceived: (Uri) -> Unit = remember {
        { viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(it)) }
    }
    val fileSaverLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            exportVaultLocationReceived.invoke(it.uri)
        }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExportVaultEvent.NavigateBack -> onNavigateBack()

            is ExportVaultEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
            }

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
            title = stringResource(id = R.string.export_vault_confirmation_title),
            message = if (state.exportFormat == ExportVaultFormat.JSON_ENCRYPTED) {
                stringResource(id = R.string.enc_export_key_warning)
                    .plus("\n\n")
                    .plus(stringResource(id = R.string.enc_export_account_warning))
            } else {
                stringResource(
                    id = R.string.export_vault_warning,
                )
            },
            confirmButtonText = stringResource(id = R.string.export_vault),
            dismissButtonText = stringResource(id = R.string.cancel),
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
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title ?: R.string.an_error_has_occurred.asText(),
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ExportVaultAction.DialogDismiss) }
                },
            )
        }

        is ExportVaultState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialog.message,
                ),
            )
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
                title = stringResource(id = R.string.export_vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ExportVaultAction.CloseButtonClick) }
                },
            )
        },
    ) { innerPadding ->
        ExportVaultScreenContent(
            state = state,
            onExportFormatOptionSelected = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.ExportFormatOptionSelect(it)) }
            },
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(it)) }
            },
            onExportVaultClick = { shouldShowConfirmationDialog = true },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun ExportVaultScreenContent(
    state: ExportVaultState,
    onExportFormatOptionSelected: (ExportVaultFormat) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onExportVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {

        if (state.policyPreventsExport) {
            BitwardenPolicyWarningText(
                text = stringResource(id = R.string.disable_personal_vault_export_policy_in_effect),
                modifier = Modifier
                    .semantics { testTag = "DisablePrivateVaultPolicyLabel" }
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.file_format),
            options = ExportVaultFormat.entries.map { it.displayLabel }.toImmutableList(),
            selectedOption = state.exportFormat.displayLabel,
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ExportVaultFormat
                    .entries
                    .first { it.displayLabel == selectedOptionLabel }
                onExportFormatOptionSelected(selectedOption)
            },
            isEnabled = !state.policyPreventsExport,
            modifier = Modifier
                .semantics { testTag = "FileFormatPicker" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            value = state.passwordInput,
            readOnly = state.policyPreventsExport,
            onValueChange = onPasswordInputChanged,
            modifier = Modifier
                .semantics { testTag = "MasterPasswordEntry" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.export_vault_master_password_description),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenFilledTonalButton(
            label = stringResource(id = R.string.export_vault),
            onClick = onExportVaultClick,
            isEnabled = !state.policyPreventsExport,
            modifier = Modifier
                .semantics { testTag = "ExportVaultButton" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
    }
}
