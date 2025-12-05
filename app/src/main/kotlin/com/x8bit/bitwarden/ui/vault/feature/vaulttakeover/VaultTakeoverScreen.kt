package com.x8bit.bitwarden.ui.vault.feature.vaulttakeover

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level screen component for the VaultTakeover screen.
 */
@Composable
fun VaultTakeoverScreen(
    onNavigateToVault: () -> Unit,
    onNavigateToLeaveOrganization: () -> Unit,
    viewModel: VaultTakeoverViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            VaultTakeoverEvent.NavigateToVault -> onNavigateToVault()
            VaultTakeoverEvent.NavigateToLeaveOrganization -> onNavigateToLeaveOrganization()
            is VaultTakeoverEvent.LaunchUri -> intentManager.launchUri(event.uri.toUri())
        }
    }

    val onContinueClick = remember(viewModel) {
        { viewModel.trySendAction(VaultTakeoverAction.ContinueClicked) }
    }
    val onDeclineClick = remember(viewModel) {
        { viewModel.trySendAction(VaultTakeoverAction.DeclineAndLeaveClicked) }
    }
    val onHelpClick = remember(viewModel) {
        { viewModel.trySendAction(VaultTakeoverAction.HelpLinkClicked) }
    }

    BitwardenScaffold {
        VaultTakeoverContent(
            organizationName = state.organizationName,
            onContinueClick = onContinueClick,
            onDeclineClick = onDeclineClick,
            onHelpClick = onHelpClick,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun VaultTakeoverContent(
    organizationName: String,
    onContinueClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = BitwardenDrawable.ic_bw_passkey),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        VaultTakeoverTextContent(organizationName = organizationName)
        Spacer(modifier = Modifier.height(24.dp))
        VaultTakeoverActions(
            onContinueClick = onContinueClick,
            onDeclineClick = onDeclineClick,
            onHelpClick = onHelpClick,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun VaultTakeoverTextContent(
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
private fun VaultTakeoverActions(
    onContinueClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.continue_text),
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
private fun VaultTakeoverScreen_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            VaultTakeoverContent(
                organizationName = "Test Organization",
                onContinueClick = {},
                onDeclineClick = {},
                onHelpClick = {},
            )
        }
    }
}
