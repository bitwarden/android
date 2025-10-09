package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.BrowserAutofillSettingsCard
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import com.x8bit.bitwarden.ui.platform.manager.utils.startBrowserAutofillSettingsActivity
import kotlinx.collections.immutable.persistentListOf

/**
 * Top level composable for the Setup Browser Autofill screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SetupBrowserAutofillScreen(
    viewModel: SetupBrowserAutofillViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SetupBrowserAutofillEvent.NavigateBack -> onNavigateBack()
            is SetupBrowserAutofillEvent.NavigateToBrowserAutofillSettings -> {
                intentManager.startBrowserAutofillSettingsActivity(
                    browserPackage = event.browserPackage,
                )
            }

            SetupBrowserAutofillEvent.NavigateToBrowserIntegrationsInfo -> {
                intentManager.launchUri(
                    "https://bitwarden.com/help/auto-fill-android/#browser-integrations/".toUri(),
                )
            }
        }
    }
    SetupBrowserAutofillDialogs(
        dialogState = state.dialogState,
        onDismissDialog = remember(viewModel) {
            { viewModel.trySendAction(SetupBrowserAutofillAction.DismissDialog) }
        },
        onTurnOnLaterConfirm = remember(viewModel) {
            { viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterConfirmClick) }
        },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(
                    id = if (state.isInitialSetup) {
                        BitwardenString.account_setup
                    } else {
                        BitwardenString.autofill_setup
                    },
                ),
                scrollBehavior = scrollBehavior,
                navigationIcon = if (state.isInitialSetup) {
                    null
                } else {
                    NavigationIcon(
                        navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                        navigationIconContentDescription = stringResource(BitwardenString.close),
                        onNavigationIconClick = remember(viewModel) {
                            { viewModel.trySendAction(SetupBrowserAutofillAction.CloseClick) }
                        },
                    )
                },
            )
        },
    ) {
        SetupBrowserAutofillContent(
            state = state,
            onWhyIsThisStepRequiredClick = remember(viewModel) {
                { viewModel.trySendAction(SetupBrowserAutofillAction.WhyIsThisStepRequiredClick) }
            },
            onBrowserClick = remember(viewModel) {
                { viewModel.trySendAction(SetupBrowserAutofillAction.BrowserIntegrationClick(it)) }
            },
            onContinueClick = remember(viewModel) {
                { viewModel.trySendAction(SetupBrowserAutofillAction.ContinueClick) }
            },
            onTurnOnLaterClick = remember(viewModel) {
                { viewModel.trySendAction(SetupBrowserAutofillAction.TurnOnLaterClick) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SetupBrowserAutofillContent(
    state: SetupBrowserAutofillState,
    onWhyIsThisStepRequiredClick: () -> Unit,
    onBrowserClick: (BrowserPackage) -> Unit,
    onContinueClick: () -> Unit,
    onTurnOnLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = BitwardenString.turn_on_browser_autofill_integration),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(Modifier.height(height = 8.dp))
        Text(
            text = stringResource(
                id = if (state.browserCount > 1) {
                    BitwardenString.youre_using_a_browser_that_requires_special_permissions_plural
                } else {
                    BitwardenString.youre_using_a_browser_that_requires_special_permissions_singular
                },
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenClickableText(
            label = stringResource(id = BitwardenString.why_is_this_step_required),
            style = BitwardenTheme.typography.labelMedium,
            onClick = onWhyIsThisStepRequiredClick,
            modifier = Modifier
                .wrapContentWidth()
                .align(alignment = Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BrowserAutofillSettingsCard(
            options = state.browserAutofillSettingsOptions,
            onOptionClicked = onBrowserClick,
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.continue_text),
            onClick = onContinueClick,
            isEnabled = state.isContinueEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        if (state.isInitialSetup) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenOutlinedButton(
                label = stringResource(BitwardenString.turn_on_later),
                onClick = onTurnOnLaterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SetupBrowserAutofillDialogs(
    dialogState: SetupBrowserAutofillState.DialogState?,
    onTurnOnLaterConfirm: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    when (dialogState) {
        SetupBrowserAutofillState.DialogState.TurnOnLaterDialog -> {
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.turn_on_browser_autofill_integration_later),
                message = stringResource(
                    id = BitwardenString.return_to_complete_this_step_anytime_in_settings,
                ),
                confirmButtonText = stringResource(id = BitwardenString.confirm),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = onTurnOnLaterConfirm,
                onDismissClick = onDismissDialog,
                onDismissRequest = onDismissDialog,
            )
        }

        null -> Unit
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetupBrowserAutofillContent_preview() {
    BitwardenTheme {
        SetupBrowserAutofillContent(
            state = SetupBrowserAutofillState(
                dialogState = null,
                isInitialSetup = true,
                browserAutofillSettingsOptions = persistentListOf(
                    BrowserAutofillSettingsOption.BraveStable(enabled = true),
                    BrowserAutofillSettingsOption.ChromeStable(enabled = false),
                    BrowserAutofillSettingsOption.ChromeBeta(enabled = true),
                ),
            ),
            onWhyIsThisStepRequiredClick = { },
            onBrowserClick = { },
            onContinueClick = { },
            onTurnOnLaterClick = { },
        )
    }
}
