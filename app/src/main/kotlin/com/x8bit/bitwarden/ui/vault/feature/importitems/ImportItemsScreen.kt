@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeImporter
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.importitems.handlers.rememberImportItemsHandler
import kotlinx.coroutines.launch

/**
 * Top level component for the import items screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportItemsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
    viewModel: ImportItemsViewModel = hiltViewModel(),
    credentialExchangeImporter: CredentialExchangeImporter =
        LocalCredentialExchangeImporter.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val handler = rememberImportItemsHandler(viewModel = viewModel)

    EventsEffect(viewModel) { event ->
        when (event) {
            ImportItemsEvent.NavigateBack -> onNavigateBack()
            ImportItemsEvent.NavigateToVault -> onNavigateToVault()
            is ImportItemsEvent.ShowRegisteredImportSources -> {
                coroutineScope.launch {
                    viewModel.trySendAction(
                        action = ImportItemsAction.ImportCredentialSelectionReceive(
                            selectionResult = credentialExchangeImporter
                                .importCredentials(
                                    credentialTypes = event.credentialTypes,
                                ),
                        ),
                    )
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(BitwardenString.import_items),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                    onNavigationIconClick = handler.onNavigateBack,
                    navigationIconContentDescription = stringResource(BitwardenString.back),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Crossfade(
            targetState = state.viewState,
            label = "CrossfadeBetweenViewStates",
            modifier = Modifier.fillMaxSize(),
        ) { viewState ->
            when (viewState) {
                ImportItemsState.ViewState.NotStarted -> {
                    GetStartedContent(
                        onGetStartedClick = remember {
                            {
                                viewModel.trySendAction(
                                    ImportItemsAction.GetStartedClick,
                                )
                            }
                        },
                    )
                }

                ImportItemsState.ViewState.AwaitingSelection -> {
                    AwaitingSelectionContent()
                }

                is ImportItemsState.ViewState.ImportingItems -> {
                    ImportingContent(viewState = viewState)
                }

                is ImportItemsState.ViewState.Completed -> {
                    CompletedContent(
                        title = viewState.title,
                        message = viewState.message,
                        iconData = viewState.iconData,
                        onReturnToVaultClick = remember {
                            {
                                onNavigateToVault()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GetStartedContent(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 24.dp)
            .standardHorizontalMargin()
            .cardStyle(CardStyle.Full),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.il_import_saved_items),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(size = 112.dp)
                .align(alignment = Alignment.CenterHorizontally),
        )

        Text(
            text = stringResource(BitwardenString.import_saved_items),
            style = BitwardenTheme.typography.titleMedium,
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        Text(
            text = stringResource(
                BitwardenString.import_your_credentials_from_another_password_manager,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        BitwardenFilledButton(
            label = stringResource(BitwardenString.get_started),
            onClick = onGetStartedClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun AwaitingSelectionContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .standardHorizontalMargin()
            .cardStyle(CardStyle.Full),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(BitwardenString.select_a_credential_manager_to_import_items_from),
            style = BitwardenTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ImportingContent(
    viewState: ImportItemsState.ViewState.ImportingItems,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .standardHorizontalMargin()
            .cardStyle(CardStyle.Full),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(BitwardenString.importing_your_saved_items),
            style = BitwardenTheme.typography.titleMedium,
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        LinearProgressIndicator(
            progress = { viewState.progress },
            color = BitwardenTheme.colorScheme.stroke.border,
            trackColor = BitwardenTheme.colorScheme.background.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .nullableTestTag("ImportProgressIndicator"),
        )

        Spacer(Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun CompletedContent(
    title: Text,
    message: Text,
    iconData: IconData,
    onReturnToVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .standardHorizontalMargin()
            .cardStyle(CardStyle.Full),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BitwardenIcon(
            iconData = iconData,
            modifier = Modifier
                .size(42.dp),
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        Text(
            text = title(),
            style = BitwardenTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        Text(
            text = message(),
            style = BitwardenTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.padding(vertical = 8.dp))

        BitwardenFilledButton(
            label = stringResource(BitwardenString.return_to_your_vault),
            onClick = onReturnToVaultClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun GetStartedContext_preview() {
    BitwardenTheme {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        BitwardenScaffold(
            topBar = {
                BitwardenTopAppBar(
                    title = stringResource(BitwardenString.import_items),
                    navigationIcon = NavigationIcon(
                        navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                        onNavigationIconClick = {},
                        navigationIconContentDescription = stringResource(BitwardenString.back),
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) {
            GetStartedContent(onGetStartedClick = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ImportingContent_preview() {
    BitwardenTheme {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        BitwardenScaffold(
            topBar = {
                BitwardenTopAppBar(
                    title = stringResource(BitwardenString.import_items),
                    navigationIcon = NavigationIcon(
                        navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                        onNavigationIconClick = {},
                        navigationIconContentDescription = stringResource(BitwardenString.back),
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) {
            ImportingContent(
                viewState = ImportItemsState.ViewState.ImportingItems(progress = 0.5f),
            )
        }
    }
}

@Preview(showBackground = true, name = "Import Success")
@Composable
private fun CompletedContentImportSuccess_preview() {
    BitwardenTheme {
        CompletedContent(
            title = BitwardenString.import_successful.asText(),
            message = BitwardenString
                .your_items_have_been_successfully_imported.asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_plain_checkmark),
            onReturnToVaultClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Import Success - Sync Fail")
@Composable
private fun CompletedContentImportSuccessSyncFail_preview() {
    BitwardenTheme {
        CompletedContent(
            title = BitwardenString.vault_sync_failed.asText(),
            message = BitwardenString
                .your_items_have_been_successfully_imported_but_could_not_sync_vault
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_refresh),
            onReturnToVaultClick = {},
        )
    }
}

@Preview(showBackground = true, name = "No Items")
@Composable
private fun CompletedContentNoItems_preview() {
    BitwardenTheme {
        CompletedContent(
            title = BitwardenString.no_items_imported.asText(),
            message = BitwardenString
                .no_items_received_from_the_selected_credential_manager
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_question_circle),
            onReturnToVaultClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Import Cancelled")
@Composable
private fun CompletedContentImportCancelled_preview() {
    BitwardenTheme {
        CompletedContent(
            title = BitwardenString.import_cancelled.asText(),
            message = BitwardenString
                .credential_import_was_cancelled
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_close),
            onReturnToVaultClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Import Fail")
@Composable
private fun CompletedContentImportFail_preview() {
    BitwardenTheme {
        CompletedContent(
            title = BitwardenString.import_error.asText(),
            message = BitwardenString
                .there_was_a_problem_importing_your_items
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_warning),
            onReturnToVaultClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AwaitingSelectionContent_preview() {
    BitwardenTheme {
        AwaitingSelectionContent()
    }
}
