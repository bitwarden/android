package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.handlers.TrustedDeviceHandlers

/**
 * The top level composable for the Reset Password screen.
 */
@Composable
fun TrustedDeviceScreen(
    onNavigateToAdminApproval: (emailAddress: String) -> Unit,
    onNavigateToLoginWithOtherDevice: (emailAddress: String) -> Unit,
    onNavigateToLock: (emailAddress: String) -> Unit,
    viewModel: TrustedDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = remember(viewModel) { TrustedDeviceHandlers.create(viewModel = viewModel) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is TrustedDeviceEvent.NavigateToApproveWithAdmin -> {
                onNavigateToAdminApproval(event.email)
            }

            is TrustedDeviceEvent.NavigateToApproveWithDevice -> {
                onNavigateToLoginWithOtherDevice(event.email)
            }

            is TrustedDeviceEvent.NavigateToLockScreen -> {
                onNavigateToLock(event.email)
            }
        }
    }

    TrustedDeviceDialogs(
        dialogState = state.dialogState,
        handlers = handlers,
    )

    TrustedDeviceScaffold(
        state = state,
        handlers = handlers,
    )
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrustedDeviceScaffold(
    state: TrustedDeviceState,
    handlers: TrustedDeviceHandlers,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.log_in_initiated),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                    navigationIconContentDescription = stringResource(id = BitwardenString.close),
                    onNavigationIconClick = handlers.onBackClick,
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenSwitch(
                label = stringResource(id = BitwardenString.remember_this_device),
                supportingText = stringResource(id = BitwardenString.turn_off_using_public_device),
                isChecked = state.isRemembered,
                onCheckedChange = handlers.onRememberToggle,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("RememberThisDeviceSwitch")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (state.showContinueButton) {
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.continue_text),
                    onClick = handlers.onContinueClick,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.showOtherDeviceButton) {
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.approve_with_my_other_device),
                    onClick = handlers.onApproveWithDeviceClick,
                    modifier = Modifier
                        .testTag("ApproveWithOtherDeviceButton")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.showRequestAdminButton) {
                BitwardenOutlinedButton(
                    label = stringResource(id = BitwardenString.request_admin_approval),
                    onClick = handlers.onApproveWithAdminClick,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .testTag("RequestAdminApprovalButton"),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.showMasterPasswordButton) {
                BitwardenOutlinedButton(
                    label = stringResource(id = BitwardenString.approve_with_master_password),
                    onClick = handlers.onApproveWithPasswordClick,
                    modifier = Modifier
                        .testTag("ApproveWithMasterPasswordButton")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    id = BitwardenString.logging_in_as_x_on_y,
                    state.emailAddress,
                    state.environmentLabel,
                ),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("LoggingInAsLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            BitwardenClickableText(
                label = stringResource(id = BitwardenString.not_you),
                onClick = handlers.onNotYouButtonClick,
                style = BitwardenTheme.typography.labelLarge,
                innerPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .testTag("NotYouLabel"),
            )

            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun TrustedDeviceDialogs(
    dialogState: TrustedDeviceState.DialogState?,
    handlers: TrustedDeviceHandlers,
) {
    when (dialogState) {
        is TrustedDeviceState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            throwable = dialogState.error,
            onDismissRequest = handlers.onDismissDialog,
        )

        is TrustedDeviceState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}

@Preview
@Composable
private fun TrustedDeviceScaffold_preview() {
    BitwardenTheme {
        TrustedDeviceScaffold(
            state = TrustedDeviceState(
                dialogState = null,
                isRemembered = false,
                emailAddress = "email@bitwarden.com",
                environmentLabel = "vault.bitwarden.pw",
                showContinueButton = false,
                showOtherDeviceButton = true,
                showRequestAdminButton = true,
                showMasterPasswordButton = true,
            ),
            handlers = TrustedDeviceHandlers(
                onBackClick = {},
                onDismissDialog = {},
                onRememberToggle = {},
                onContinueClick = {},
                onApproveWithAdminClick = {},
                onApproveWithDeviceClick = {},
                onApproveWithPasswordClick = {},
                onNotYouButtonClick = {},
            ),
        )
    }
}
