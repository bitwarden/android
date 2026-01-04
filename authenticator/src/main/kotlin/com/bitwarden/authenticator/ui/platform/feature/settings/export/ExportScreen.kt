package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.authenticator.ui.platform.util.displayLabel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Top level composable for the export data screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val exportLocationReceive: (Uri) -> Unit = remember {
        {
            viewModel.trySendAction(ExportAction.ExportLocationReceive(it))
        }
    }
    val fileSaveLauncher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            exportLocationReceive.invoke(it.uri)
        }
    }
    val snackbarState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExportEvent.NavigateBack -> onNavigateBack()
            is ExportEvent.ShowSnackBar -> {
                snackbarState.showSnackbar(BitwardenSnackbarData(message = event.message))
            }

            is ExportEvent.NavigateToSelectExportDestination -> {
                fileSaveLauncher.launch(
                    intentManager.createDocumentIntent(
                        fileName = event.fileName,
                    ),
                )
            }
        }
    }

    var shouldShowConfirmationPrompt by remember { mutableStateOf(false) }
    val confirmExportClick = remember(viewModel) {
        {
            viewModel.trySendAction(ExportAction.ConfirmExportClick)
        }
    }
    if (shouldShowConfirmationPrompt) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.export_confirmation_title),
            message = stringResource(
                id = BitwardenString.export_vault_warning,
            ),
            confirmButtonText = stringResource(id = BitwardenString.export),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowConfirmationPrompt = false
                confirmExportClick()
            },
            onDismissClick = { shouldShowConfirmationPrompt = false },
            onDismissRequest = { shouldShowConfirmationPrompt = false },
        )
    }

    when (val dialog = state.dialogState) {
        is ExportState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                onDismissRequest = remember(viewModel) {
                    {
                        viewModel.trySendAction(ExportAction.DialogDismiss)
                    }
                },
            )
        }

        is ExportState.DialogState.Loading -> {
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
                title = stringResource(id = BitwardenString.export),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ExportAction.CloseButtonClick)
                    }
                },
            )
        },
        snackbarHost = { BitwardenSnackbarHost(snackbarState) },
    ) {
        ExportScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onExportFormatOptionSelected = remember(viewModel) {
                {
                    viewModel.trySendAction(ExportAction.ExportFormatOptionSelect(it))
                }
            },
            onExportClick = { shouldShowConfirmationPrompt = true },
        )
    }
}

@Composable
private fun ExportScreenContent(
    modifier: Modifier = Modifier,
    state: ExportState,
    onExportFormatOptionSelected: (ExportVaultFormat) -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        val resources = LocalResources.current
        Spacer(modifier = Modifier.height(height = 24.dp))

        Text(
            text = stringResource(id = BitwardenString.included_in_this_export),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(
                id = BitwardenString.only_codes_stored_locally_on_this_device_will_be_exported,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.file_format),
            options = ExportVaultFormat.entries.map { it.displayLabel() }.toImmutableList(),
            selectedOption = state.exportVaultFormat.displayLabel(),
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ExportVaultFormat
                    .entries
                    .first { it.displayLabel(resources) == selectedOptionLabel }
                onExportFormatOptionSelected(selectedOption)
            },
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("FileFormatPicker")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.export),
            onClick = onExportClick,
            modifier = Modifier
                .testTag("ExportVaultButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
