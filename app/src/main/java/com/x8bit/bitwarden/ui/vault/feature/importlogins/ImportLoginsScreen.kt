package com.x8bit.bitwarden.ui.vault.feature.importlogins

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.importlogins.handlers.ImportLoginHandler
import com.x8bit.bitwarden.ui.vault.feature.importlogins.handlers.rememberImportLoginHandler

/**
 * Top level component for the import logins screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportLoginsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportLoginsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberImportLoginHandler(viewModel = viewModel)

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ImportLoginsEvent.NavigateBack -> onNavigateBack()
        }
    }

    ImportLoginsDialogContent(state = state, handler = handler)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.import_logins),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(R.drawable.ic_close),
                    onNavigationIconClick = handler.onCloseClick,
                    navigationIconContentDescription = stringResource(R.string.close),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
        ) {
            ImportLoginsContent(
                onGetStartedClick = handler.onGetStartedClick,
                onImportLaterClick = handler.onImportLaterClick,
            )
        }
    }
}

@Composable
private fun ImportLoginsDialogContent(
    state: ImportLoginsState,
    handler: ImportLoginHandler,
) {
    val confirmButtonText = stringResource(R.string.confirm)
    val dismissButtonText = stringResource(R.string.cancel)
    when (val dialogState = state.dialogState) {
        ImportLoginsState.DialogState.GetStarted -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                onDismissRequest = handler.onDismissDialog,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
                onConfirmClick = handler.onConfirmGetStarted,
                onDismissClick = handler.onDismissDialog,
            )
        }

        ImportLoginsState.DialogState.ImportLater -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                onDismissRequest = handler.onDismissDialog,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
                onConfirmClick = handler.onConfirmImportLater,
                onDismissClick = handler.onDismissDialog,
            )
        }

        null -> Unit
    }
}

@Composable
private fun ImportLoginsContent(
    onGetStartedClick: () -> Unit,
    onImportLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberVectorPainter(R.drawable.img_import_logins),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(124.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.give_your_vault_a_head_start),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.from_your_computer_follow_these_instructions_to_export_saved_passwords,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.get_started),
            onClick = onGetStartedClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(R.string.import_logins_later),
            onClick = onImportLaterClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportLoginsInitialContent_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(
                BitwardenTheme.colorScheme.background.primary,
            ),
        ) {
            ImportLoginsContent(
                onGetStartedClick = {},
                onImportLaterClick = {},
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportLoginsScreenDialog_preview(
    @PreviewParameter(ImportLoginsDialogContentPreviewProvider::class) state: ImportLoginsState,
) {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(
                BitwardenTheme.colorScheme.background.primary,
            ),
        ) {
            ImportLoginsDialogContent(
                state = state,
                handler = ImportLoginHandler(
                    onDismissDialog = {},
                    onConfirmGetStarted = {},
                    onConfirmImportLater = {},
                    onCloseClick = {},
                    onGetStartedClick = {},
                    onImportLaterClick = {},
                ),
            )
            ImportLoginsContent(
                onGetStartedClick = {},
                onImportLaterClick = {},
            )
        }
    }
}

@OmitFromCoverage
private class ImportLoginsDialogContentPreviewProvider :
    PreviewParameterProvider<ImportLoginsState> {
    override val values: Sequence<ImportLoginsState>
        get() = sequenceOf(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.GetStarted,
            ),
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.ImportLater,
            ),
        )
}
