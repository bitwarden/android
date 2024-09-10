package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.handlers.rememberSetupAutoFillHandler
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the Auto-fill setup screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupAutoFillScreen(
    onNavigateToCompleteSetup: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: SetupAutoFillViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handler = rememberSetupAutoFillHandler(viewModel = viewModel)
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SetupAutoFillEvent.NavigateToCompleteSetup -> onNavigateToCompleteSetup()
            SetupAutoFillEvent.NavigateToAutofillSettings -> {
                val showFallback = !intentManager.startSystemAutofillSettingsActivity()
                if (showFallback) {
                    handler.sendAutoFillServiceFallback.invoke()
                }
            }
        }
    }
    when (state.dialogState) {
        is SetupAutoFillDialogState.AutoFillFallbackDialog -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = null,
                    message = R.string.bitwarden_autofill_go_to_settings.asText(),
                ),
                onDismissRequest = handler.onDismissDialog,
            )
        }

        is SetupAutoFillDialogState.TurnOnLaterDialog -> {
            BitwardenTwoButtonDialog(
                title = stringResource(R.string.turn_on_autofill_later),
                message = stringResource(R.string.return_to_complete_this_step_anytime_in_settings),
                confirmButtonText = stringResource(id = R.string.confirm),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = handler.onConfirmTurnOnLaterClick,
                onDismissClick = handler.onDismissDialog,
                onDismissRequest = handler.onDismissDialog,
            )
        }

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.account_setup),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
            )
        },
    ) { innerPadding ->
        SetupAutoFillContent(
            autofillEnabled = state.autofillEnabled,
            onAutofillServiceChanged = { handler.onAutofillServiceChanged(it) },
            onContinueClick = handler.onContinueClick,
            onTurnOnLaterClick = handler.onTurnOnLaterClick,
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SetupAutoFillContent(
    autofillEnabled: Boolean,
    onAutofillServiceChanged: (Boolean) -> Unit,
    onContinueClick: () -> Unit,
    onTurnOnLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Animated Image placeholder TODO PM-10843
        Image(
            painter = rememberVectorPainter(id = R.drawable.account_setup),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.turn_on_autofill),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .standardHorizontalMargin()
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.use_autofill_to_log_into_your_accounts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .standardHorizontalMargin()
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenWideSwitch(
            label = stringResource(
                R.string.autofill_services,
            ),
            isChecked = autofillEnabled,
            onCheckedChange = onAutofillServiceChanged,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenTextButton(
            label = stringResource(R.string.turn_on_later),
            onClick = onTurnOnLaterClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupAutoFillContentDisabled_preview() {
    BitwardenTheme {
        SetupAutoFillContent(
            autofillEnabled = false,
            onAutofillServiceChanged = {},
            onContinueClick = {},
            onTurnOnLaterClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupAutoFillContentEnabled_preview() {
    BitwardenTheme {
        SetupAutoFillContent(
            autofillEnabled = true,
            onAutofillServiceChanged = {},
            onContinueClick = {},
            onTurnOnLaterClick = {},
        )
    }
}
