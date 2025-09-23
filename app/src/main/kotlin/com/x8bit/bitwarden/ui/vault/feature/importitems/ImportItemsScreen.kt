package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeImporter
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.importitems.ImportItemsAction.ImportCredentialSelectionReceive
import com.x8bit.bitwarden.ui.vault.feature.importitems.handlers.rememberImportItemsHandler
import kotlinx.coroutines.launch

/**
 * Top level component for the import items screen.
 */
@Suppress("LongMethod")
@Composable
fun ImportItemsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImportFromComputer: () -> Unit,
    viewModel: ImportItemsViewModel = hiltViewModel(),
    credentialExchangeImporter: CredentialExchangeImporter =
        LocalCredentialExchangeImporter.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val handler = rememberImportItemsHandler(viewModel = viewModel)
    val snackbarHostState = rememberBitwardenSnackbarHostState()

    EventsEffect(viewModel) { event ->
        when (event) {
            ImportItemsEvent.NavigateBack -> onNavigateBack()
            ImportItemsEvent.NavigateToImportFromComputer -> onNavigateToImportFromComputer()
            is ImportItemsEvent.ShowRegisteredImportSources -> {
                coroutineScope.launch {
                    viewModel.trySendAction(
                        action = ImportCredentialSelectionReceive(
                            selectionResult = credentialExchangeImporter
                                .importCredentials(
                                    credentialTypes = event.credentialTypes,
                                ),
                        ),
                    )
                }
            }

            is ImportItemsEvent.ShowBasicSnackbar -> {
                snackbarHostState.showSnackbar(event.data)
            }

            is ImportItemsEvent.ShowSyncFailedSnackbar -> {
                snackbarHostState.showSnackbar(
                    snackbarData = event.data,
                    onActionPerformed = handler.onSyncFailedTryAgainClick,
                )
            }
        }
    }

    ImportItemsDialogs(
        dialog = state.dialog,
        onDismissDialog = handler.onDismissDialog,
    )

    ImportItemsScaffold(
        onNavigateBack = handler.onNavigateBack,
        onImportFromComputerClick = handler.onImportFromComputerClick,
        onImportFromAnotherAppClick = handler.onImportFromAnotherAppClick,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportItemsScaffold(
    onNavigateBack: () -> Unit,
    onImportFromComputerClick: () -> Unit,
    onImportFromAnotherAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: BitwardenSnackbarHostState = rememberBitwardenSnackbarHostState(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(BitwardenString.import_items),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                    onNavigationIconClick = onNavigateBack,
                    navigationIconContentDescription = stringResource(BitwardenString.back),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(
                bitwardenHostState = snackbarHostState,
            )
        },
    ) {
        ImportItemsContent(
            onImportFromComputerClick = onImportFromComputerClick,
            onImportFromAnotherAppClick = onImportFromAnotherAppClick,
            modifier = Modifier
                .fillMaxSize()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun ImportItemsContent(
    onImportFromComputerClick: () -> Unit,
    onImportFromAnotherAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        item {
            BitwardenTextRow(
                text = stringResource(BitwardenString.import_from_computer),
                onClick = onImportFromComputerClick,
                cardStyle = CardStyle.Top(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            BitwardenTextRow(
                text = stringResource(BitwardenString.import_from_another_app),
                onClick = onImportFromAnotherAppClick,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun ImportItemsDialogs(
    dialog: ImportItemsState.DialogState?,
    onDismissDialog: () -> Unit,
) {
    when (dialog) {
        is ImportItemsState.DialogState.General -> {
            BitwardenBasicDialog(
                title = dialog.title(),
                message = dialog.message(),
                onDismissRequest = onDismissDialog,
                throwable = dialog.throwable,
            )
        }

        is ImportItemsState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }
}

//region Previews

@Preview(showBackground = true, name = "Initial state")
@Composable
private fun ImportItemsContent_preview() {
    BitwardenTheme {
        ImportItemsScaffold(
            onNavigateBack = {},
            onImportFromComputerClick = {},
            onImportFromAnotherAppClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading dialog")
@Composable
private fun ImportItemsDialogs_loading_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            ImportItemsDialogs(
                dialog = ImportItemsState.DialogState.Loading("Decoding items...".asText()),
                onDismissDialog = {},
            )
        }
    }
}
//endregion Previews
