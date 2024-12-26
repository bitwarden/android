package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ContinueDialogClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.DismissDialogClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.RemindMeLaterClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateBackToVault
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the new device notice two factor screen.
 */
@Composable
fun NewDeviceNoticeTwoFactorScreen(
    onNavigateBackToVault: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: NewDeviceNoticeTwoFactorViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is NavigateToTurnOnTwoFactor -> {
                intentManager.launchUri(event.url.toUri())
            }

            is NavigateToChangeAccountEmail -> {
                intentManager.launchUri(event.url.toUri())
            }

            NavigateBackToVault -> onNavigateBackToVault()
        }
    }

    // Show dialog if needed:
    when (val dialogState = state.dialogState) {
        is NewDeviceNoticeTwoFactorDialogState.TurnOnTwoFactorDialog,
        is NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog,
            ->
            BitwardenTwoButtonDialog(
                title = stringResource(R.string.continue_to_web_app),
                message = dialogState.message(),
                confirmButtonText = stringResource(id = R.string.confirm),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = { viewModel.trySendAction(ContinueDialogClick) },
                onDismissClick = { viewModel.trySendAction(DismissDialogClick) },
                onDismissRequest = { viewModel.trySendAction(DismissDialogClick) },
            )

        null -> Unit
    }

    BitwardenScaffold {
        NewDeviceNoticeTwoFactorContent(
            onTurnOnTwoFactorClick = {
                viewModel.trySendAction(TurnOnTwoFactorClick)
            },
            onChangeAccountEmailClick = {
                viewModel.trySendAction(ChangeAccountEmailClick)
            },
            onRemindMeLaterClick = {
                viewModel.trySendAction(RemindMeLaterClick)
            },
            state = state,
        )
    }
}

/**
 * The content of the screen.
 */
@Composable
private fun NewDeviceNoticeTwoFactorContent(
    onTurnOnTwoFactorClick: () -> Unit,
    onChangeAccountEmailClick: () -> Unit,
    onRemindMeLaterClick: () -> Unit,
    state: NewDeviceNoticeTwoFactorState,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .standardHorizontalMargin()
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(104.dp))
        HeaderContent()
        Spacer(modifier = Modifier.height(24.dp))
        MainContent(
            onTurnOnTwoFactorClick = onTurnOnTwoFactorClick,
            onChangeAccountEmailClick = onChangeAccountEmailClick,
            onRemindMeLaterClick = onRemindMeLaterClick,
            state = state,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Header content containing the user lock icon and title.
 */
@Suppress("MaxLineLength")
@Composable
private fun ColumnScope.HeaderContent() {
    Image(
        painter = rememberVectorPainter(id = R.drawable.user_lock),
        contentDescription = null,
        modifier = Modifier.size(120.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.set_up_two_step_login),
        style = BitwardenTheme.typography.titleMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(
            R.string.you_can_set_up_two_step_login_as_an_alternative_way_to_protect_your_account_or_change_your_email_to_one_you_can_access,
        ),
        style = BitwardenTheme.typography.bodyMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        textAlign = TextAlign.Center,
    )
}

/**
 * The content containing the external links and remind me buttons.
 */
@Composable
private fun ColumnScope.MainContent(
    onTurnOnTwoFactorClick: () -> Unit,
    onChangeAccountEmailClick: () -> Unit,
    onRemindMeLaterClick: () -> Unit,
    state: NewDeviceNoticeTwoFactorState,
) {
    BitwardenFilledButton(
        label = stringResource(R.string.turn_on_two_step_login),
        onClick = onTurnOnTwoFactorClick,
        icon = rememberVectorPainter(id = R.drawable.ic_external_link),
        modifier = Modifier
            .fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    BitwardenOutlinedButton(
        label = stringResource(R.string.change_account_email),
        onClick = onChangeAccountEmailClick,
        icon = rememberVectorPainter(id = R.drawable.ic_external_link),
        modifier = Modifier
            .fillMaxWidth(),
    )
    if (state.shouldShowRemindMeLater) {
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(R.string.remind_me_later),
            onClick = onRemindMeLaterClick,
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun NewDeviceNoticeTwoFactorScreen_preview() {
    BitwardenTheme {
        NewDeviceNoticeTwoFactorContent(
            onTurnOnTwoFactorClick = {},
            onChangeAccountEmailClick = {},
            onRemindMeLaterClick = {},
            state = NewDeviceNoticeTwoFactorState(
                shouldShowRemindMeLater = true,
            ),
        )
    }
}
