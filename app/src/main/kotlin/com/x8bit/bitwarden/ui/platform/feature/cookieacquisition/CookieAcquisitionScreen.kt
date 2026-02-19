package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScaffoldDefaults
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
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.handlers.CookieAcquisitionHandler
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.handlers.rememberCookieAcquisitionHandler

/**
 * Top-level composable for the Cookie Acquisition screen.
 */
@Composable
fun CookieAcquisitionScreen(
    onDismiss: () -> Unit,
    viewModel: CookieAcquisitionViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is CookieAcquisitionEvent.LaunchBrowser -> {
                intentManager.startCustomTabsActivity(event.uri.toUri())
            }

            is CookieAcquisitionEvent.NavigateToHelp -> {
                intentManager.launchUri(event.uri.toUri())
            }

            CookieAcquisitionEvent.NavigateBack -> onDismiss()
        }
    }

    val handler = rememberCookieAcquisitionHandler(viewModel = viewModel)

    // Route back through the ViewModel so the pending cookie request is cleared
    // before dismissing. A normal back-pop would leave the request active and
    // MainViewModel would immediately re-navigate to this screen.
    BackHandler {
        viewModel.trySendAction(CookieAcquisitionAction.ContinueWithoutSyncingClick)
    }

    CookieAcquisitionDialogs(
        dialogState = state.dialogState,
        onDismissRequest = handler.onDismissDialogClick,
    )

    BitwardenScaffold(
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    ) {
        CookieAcquisitionContent(
            state = state,
            handler = handler,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CookieAcquisitionDialogs(
    dialogState: CookieAcquisitionDialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is CookieAcquisitionDialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun CookieAcquisitionContent(
    state: CookieAcquisitionState,
    handler: CookieAcquisitionHandler,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_sso_cookie_sync),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = BitwardenString.sync_with_browser),
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
                id = BitwardenString.sync_with_browser_description,
                state.environmentUrl,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.launch_browser),
            onClick = handler.onLaunchBrowserClick,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.continue_without_syncing),
            onClick = handler.onContinueWithoutSyncingClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenTextButton(
            label = stringResource(id = BitwardenString.why_am_i_seeing_this),
            onClick = handler.onWhyAmISeeingThisClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun CookieAcquisitionScreen_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            CookieAcquisitionContent(
                state = CookieAcquisitionState(
                    environmentUrl = "vault.bitwarden.com",
                    dialogState = null,
                ),
                handler = CookieAcquisitionHandler(
                    onLaunchBrowserClick = {},
                    onContinueWithoutSyncingClick = {},
                    onWhyAmISeeingThisClick = {},
                    onDismissDialogClick = {},
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CookieAcquisitionScreen_darkPreview() {
    BitwardenTheme(theme = AppTheme.DARK) {
        BitwardenScaffold {
            CookieAcquisitionContent(
                state = CookieAcquisitionState(
                    environmentUrl = "vault.bitwarden.com",
                    dialogState = null,
                ),
                handler = CookieAcquisitionHandler(
                    onLaunchBrowserClick = {},
                    onContinueWithoutSyncingClick = {},
                    onWhyAmISeeingThisClick = {},
                    onDismissDialogClick = {},
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
