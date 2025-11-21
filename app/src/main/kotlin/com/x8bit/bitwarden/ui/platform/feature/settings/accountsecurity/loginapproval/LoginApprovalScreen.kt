package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the login approval screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun LoginApprovalScreen(
    viewModel: LoginApprovalViewModel = hiltViewModel(),
    exitManager: ExitManager = LocalExitManager.current,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginApprovalEvent.ExitApp -> exitManager.exitApplication()
            LoginApprovalEvent.NavigateBack -> onNavigateBack()
        }
    }

    LoginApprovalDialogs(
        state = state.dialogState,
        onDismissError = remember(viewModel) {
            { viewModel.trySendAction(LoginApprovalAction.ErrorDialogDismiss) }
        },
        onConfirmChangeAccount = remember(viewModel) {
            { viewModel.trySendAction(LoginApprovalAction.ApproveAccountChangeClick) }
        },
        onDismissChangeAccount = remember(viewModel) {
            { viewModel.trySendAction(LoginApprovalAction.CancelAccountChangeClick) }
        },
    )

    BackHandler(
        onBack = remember(viewModel) {
            { viewModel.trySendAction(LoginApprovalAction.CloseClick) }
        },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.log_in_requested),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginApprovalAction.CloseClick) }
                },
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is LoginApprovalState.ViewState.Content -> {
                LoginApprovalContent(
                    state = viewState,
                    onConfirmLoginClick = remember(viewModel) {
                        { viewModel.trySendAction(LoginApprovalAction.ApproveRequestClick) }
                    },
                    onDeclineLoginClick = remember(viewModel) {
                        { viewModel.trySendAction(LoginApprovalAction.DeclineRequestClick) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is LoginApprovalState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.generic_error_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is LoginApprovalState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun LoginApprovalContent(
    state: LoginApprovalState.ViewState.Content,
    onConfirmLoginClick: () -> Unit,
    onDeclineLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(state = rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = BitwardenString.are_you_trying_to_log_in),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = stringResource(
                id = BitwardenString.log_in_attempt_by_x_on_y,
                state.email,
                state.domainUrl,
            ),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("LogInAttemptByLabel"),
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = BitwardenString.fingerprint_phrase),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(
                    cardStyle = CardStyle.Top(hasDivider = false),
                    paddingBottom = 4.dp,
                )
                .padding(horizontal = 16.dp),
        )
        Text(
            text = state.fingerprint,
            textAlign = TextAlign.Start,
            color = BitwardenTheme.colorScheme.text.codePink,
            style = BitwardenTheme.typography.sensitiveInfoSmall,
            modifier = Modifier
                .testTag("FingerprintValueLabel")
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(
                    cardStyle = CardStyle.Middle(dividerPadding = 0.dp),
                    paddingTop = 0.dp,
                    paddingStart = 16.dp,
                    paddingEnd = 16.dp,
                ),
        )

        LoginApprovalInfoColumn(
            label = stringResource(id = BitwardenString.device_type),
            value = state.deviceType,
            valueTestTag = "DeviceTypeValueLabel",
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(cardStyle = CardStyle.Middle()),
        )

        LoginApprovalInfoColumn(
            label = stringResource(id = BitwardenString.ip_address),
            value = state.ipAddress,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(cardStyle = CardStyle.Middle()),
        )

        LoginApprovalInfoColumn(
            label = stringResource(id = BitwardenString.time),
            value = state.time,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(cardStyle = CardStyle.Bottom),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.confirm_log_in),
            onClick = onConfirmLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("ConfirmLoginButton"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.deny_log_in),
            onClick = onDeclineLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("DenyLoginButton"),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * A view displaying information about this login approval request.
 */
@Composable
private fun LoginApprovalInfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueTestTag: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = BitwardenTheme.typography.titleSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 4.dp))

        Text(
            text = value,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .nullableTestTag(tag = valueTestTag),
        )
    }
}

@Composable
private fun LoginApprovalDialogs(
    state: LoginApprovalState.DialogState?,
    onDismissError: () -> Unit,
    onConfirmChangeAccount: () -> Unit,
    onDismissChangeAccount: () -> Unit,
) {
    when (state) {
        is LoginApprovalState.DialogState.ChangeAccount -> BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.log_in_requested),
            message = state.message(),
            confirmButtonText = stringResource(id = BitwardenString.okay),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = onConfirmChangeAccount,
            onDismissClick = onDismissChangeAccount,
            onDismissRequest = onDismissChangeAccount,
        )

        is LoginApprovalState.DialogState.Error -> BitwardenBasicDialog(
            title = state.title?.invoke(),
            message = state.message(),
            throwable = state.error,
            onDismissRequest = onDismissError,
        )

        null -> Unit
    }
}
