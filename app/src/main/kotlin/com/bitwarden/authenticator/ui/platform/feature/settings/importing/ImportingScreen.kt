package com.bitwarden.authenticator.ui.platform.feature.settings.importing

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
import androidx.compose.runtime.remember
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
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.model.ImportFormat
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.bitwarden.authenticator.ui.platform.util.displayLabel
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportingScreen(
    viewModel: ImportingViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val importLocationReceive: (IntentManager.FileData) -> Unit = remember {
        {
            viewModel.trySendAction(ImportAction.ImportLocationReceive(it))
        }
    }
    val launcher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            importLocationReceive(it)
        }
    }

    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ImportEvent.NavigateBack -> onNavigateBack()
            is ImportEvent.NavigateToSelectImportFile -> {
                launcher.launch(intentManager.createFileChooserIntent(event.importFormat.mimeType))
            }

            is ImportEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    when (val dialog = state.dialogState) {
        is ImportState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title,
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    {
                        viewModel.trySendAction(ImportAction.DialogDismiss)
                    }
                }
            )
        }

        is ImportState.DialogState.Loading -> {
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
                title = stringResource(id = R.string.import_vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(ImportAction.CloseButtonClick)
                    }
                },
            )
        },
    ) { paddingValues ->
        ImportScreenContent(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = state,
            onImportFormatOptionSelected = remember(viewModel) {
                {
                    viewModel.trySendAction(ImportAction.ImportFormatOptionSelect(it))
                }
            },
            onImportClick = remember(viewModel) {
                {
                    viewModel.trySendAction(ImportAction.ImportClick)
                }
            },
        )
    }
}

@Composable
private fun ImportScreenContent(
    modifier: Modifier = Modifier,
    state: ImportState,
    onImportFormatOptionSelected: (ImportFormat) -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        val resources = LocalContext.current.resources
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.file_format),
            options = ImportFormat.entries.map { it.displayLabel() }.toImmutableList(),
            selectedOption = state.importFormat.displayLabel(),
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ImportFormat
                    .entries
                    .first { it.displayLabel(resources) == selectedOptionLabel }
                onImportFormatOptionSelected(selectedOption)
            },
            modifier = Modifier
                .semantics { testTag = "FileFormatPicker" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenFilledTonalButton(
            label = stringResource(id = R.string.import_vault),
            onClick = onImportClick,
            modifier = Modifier
                .semantics { testTag = "ImportVaultButton" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
    }
}
