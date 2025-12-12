package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.handler.rememberMigrateToMyItemsHandler

/**
 * Top level screen component for the MigrateToMyItems screen.
 */
@Composable
fun MigrateToMyItemsScreen(
    onNavigateToVault: () -> Unit,
    onNavigateToLeaveOrganization: () -> Unit,
    viewModel: MigrateToMyItemsViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = rememberMigrateToMyItemsHandler(viewModel)

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MigrateToMyItemsEvent.NavigateToVault -> onNavigateToVault()
            MigrateToMyItemsEvent.NavigateToLeaveOrganization -> onNavigateToLeaveOrganization()
            is MigrateToMyItemsEvent.LaunchUri -> intentManager.launchUri(event.uri.toUri())
        }
    }

    MigrateToMyItemsDialogs(
        dialog = state.dialog,
        onDismissRequest = handlers.onDismissDialog,
    )

    BitwardenScaffold {
        MigrateToMyItemsContent(
            state = state,
            onAcceptClick = handlers.onAcceptClick,
            onDeclineClick = handlers.onDeclineClick,
            onHelpClick = handlers.onHelpClick,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun MigrateToMyItemsDialogs(
    dialog: MigrateToMyItemsState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialog) {
        is MigrateToMyItemsState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title(),
                message = dialog.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is MigrateToMyItemsState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }
}

@Composable
private fun MigrateToMyItemsContent(
    state: MigrateToMyItemsState,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_migrate_to_my_items),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        MigrateToMyItemsTextContent(organizationName = state.organizationName)
        Spacer(modifier = Modifier.height(24.dp))
        MigrateToMyItemsActions(
            onContinueClick = onAcceptClick,
            onDeclineClick = onDeclineClick,
            onHelpClick = onHelpClick,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MigrateToMyItemsTextContent(
    organizationName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(
                id = BitwardenString.transfer_items_to_org,
                organizationName,
            ),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                id = BitwardenString.transfer_items_description,
                organizationName,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun MigrateToMyItemsActions(
    onContinueClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.accept),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.decline_and_leave),
            onClick = onDeclineClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenTextButton(
            label = stringResource(id = BitwardenString.why_am_i_seeing_this),
            onClick = onHelpClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MigrateToMyItemsScreen_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            MigrateToMyItemsContent(
                state = MigrateToMyItemsState(
                    organizationId = "test-org-id",
                    organizationName = "Bitwarden",
                    dialog = null,
                ),
                onAcceptClick = {},
                onDeclineClick = {},
                onHelpClick = {},
            )
        }
    }
}
