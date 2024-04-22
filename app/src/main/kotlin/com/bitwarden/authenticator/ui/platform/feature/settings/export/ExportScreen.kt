package com.bitwarden.authenticator.ui.platform.feature.settings.export

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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportFormat
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.util.displayLabel
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExportEvent.NavigateBack -> onNavigateBack()
            is ExportEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
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
            title = stringResource(id = R.string.export_confirmation_title),
            message = stringResource(
                id = R.string.export_vault_warning,
            ),
            confirmButtonText = stringResource(id = R.string.export),
            dismissButtonText = stringResource(id = R.string.cancel),
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
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title,
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    {
                        viewModel.trySendAction(ExportAction.DialogDismiss)
                    }
                }
            )
        }

        is ExportState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialog.message
                )
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
                title = stringResource(id = R.string.export),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ExportAction.CloseButtonClick)
                    }
                },
            )
        },
    ) { paddingValues ->
        ExportScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = state,
            onExportFormatOptionSelected = remember(viewModel) {
                {
                    viewModel.trySendAction(ExportAction.ExportFormatOptionSelect(it))
                }
            },
            onExportClick = { shouldShowConfirmationPrompt = true }
        )
    }
}

@Composable
private fun ExportScreenContent(
    modifier: Modifier = Modifier,
    state: ExportState,
    onExportFormatOptionSelected: (ExportFormat) -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        val resources = LocalContext.current.resources
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.file_format),
            options = ExportFormat.entries.map { it.displayLabel() }.toImmutableList(),
            selectedOption = state.exportFormat.displayLabel(),
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ExportFormat
                    .entries
                    .first { it.displayLabel(resources) == selectedOptionLabel }
                onExportFormatOptionSelected(selectedOption)
            },
            modifier = Modifier
                .semantics { testTag = "FileFormatPicker" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenFilledTonalButton(
            label = stringResource(id = R.string.export),
            onClick = onExportClick,
            modifier = Modifier
                .semantics { testTag = "ExportVaultButton" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
    }
}
