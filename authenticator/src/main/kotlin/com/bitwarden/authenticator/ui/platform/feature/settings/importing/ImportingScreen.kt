package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.ui.platform.util.displayLabel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Top level composable for the importing screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportingScreen(
    viewModel: ImportingViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val importLocationReceive: (FileData) -> Unit = {
        viewModel.trySendAction(ImportAction.ImportLocationReceive(it))
    }
    val launcher = intentManager.getActivityResultLauncher { activityResult ->
        intentManager.getFileDataFromActivityResult(activityResult)?.let {
            importLocationReceive(it)
        }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ImportEvent.NavigateBack -> onNavigateBack()
            is ImportEvent.NavigateToSelectImportFile -> {
                launcher.launch(
                    intentManager.createFileChooserIntent(
                        withCameraIntents = false,
                        mimeType = event.importFileFormat.mimeType,
                    ),
                )
            }
        }
    }

    when (val dialog = state.dialogState) {
        is ImportState.DialogState.Error -> {
            BitwardenTwoButtonDialog(
                title = dialog.title?.invoke(),
                message = dialog.message.invoke(),
                confirmButtonText = stringResource(id = BitwardenString.get_help),
                onConfirmClick = {
                    intentManager.launchUri("https://bitwarden.com/help".toUri())
                },
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onDismissClick = {
                    viewModel.trySendAction(ImportAction.DialogDismiss)
                },
                onDismissRequest = {
                    viewModel.trySendAction(ImportAction.DialogDismiss)
                },
            )
        }

        is ImportState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        is ImportState.DialogState.PasswordPrompt -> {
            ImportPasswordPromptDialog(
                errorMessage = dialog.errorMessage?.invoke(),
                onConfirmClick = { viewModel.trySendAction(ImportAction.PasswordSubmit(it)) },
                onDismissRequest = {
                    viewModel.trySendAction(ImportAction.PasswordPromptDismiss)
                },
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
                title = stringResource(id = BitwardenString.import_vault),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = { viewModel.trySendAction(ImportAction.CloseButtonClick) },
            )
        },
    ) {
        ImportScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onImportFormatOptionSelected = {
                viewModel.trySendAction(ImportAction.ImportFormatOptionSelect(it))
            },
            onImportClick = { viewModel.trySendAction(ImportAction.ImportClick) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportPasswordPromptDialog(
    errorMessage: String?,
    onConfirmClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.cancel),
                onClick = onDismissRequest,
                modifier = Modifier.testTag("DismissAlertButton"),
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.okay),
                onClick = { onConfirmClick(password) },
                isEnabled = password.isNotEmpty(),
                modifier = Modifier.testTag("AcceptAlertButton"),
            )
        },
        title = {
            Text(
                text = stringResource(id = BitwardenString.this_file_is_password_protected),
                style = BitwardenTheme.typography.headlineSmall,
                modifier = Modifier.testTag("AlertTitleText"),
            )
        },
        text = {
            ImportPasswordPromptContent(
                password = password,
                errorMessage = errorMessage,
                onPasswordChange = { password = it },
                focusRequester = focusRequester,
            )
        },
        shape = BitwardenTheme.shapes.dialog,
        containerColor = BitwardenTheme.colorScheme.background.primary,
        titleContentColor = BitwardenTheme.colorScheme.text.primary,
        textContentColor = BitwardenTheme.colorScheme.text.primary,
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
            testTag = "AlertPopup"
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun ImportPasswordPromptContent(
    password: String,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Column {
        Text(
            text = stringResource(
                id = BitwardenString.enter_the_password_used_to_encrypt_this_file,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenPasswordField(
            label = stringResource(id = BitwardenString.file_password),
            value = password,
            onValueChange = onPasswordChange,
            supportingText = errorMessage,
            passwordFieldTestTag = "AlertInputField",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ImportScreenContent(
    modifier: Modifier = Modifier,
    state: ImportState,
    onImportFormatOptionSelected: (ImportFileFormat) -> Unit,
    onImportClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        val resources = LocalResources.current
        Spacer(modifier = Modifier.height(height = 12.dp))
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.file_format),
            options = ImportFileFormat.entries.map { it.displayLabel() }.toImmutableList(),
            selectedOption = state.importFileFormat.displayLabel(),
            onOptionSelected = { selectedOptionLabel ->
                val selectedOption = ImportFileFormat
                    .entries
                    .first { it.displayLabel(resources) == selectedOptionLabel }
                onImportFormatOptionSelected(selectedOption)
            },
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .testTag("FileFormatPicker")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.import_vault),
            onClick = onImportClick,
            isExternalLink = true,
            modifier = Modifier
                .testTag("ImportVaultButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
