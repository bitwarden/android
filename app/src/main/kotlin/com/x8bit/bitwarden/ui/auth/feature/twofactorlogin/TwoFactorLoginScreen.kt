package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.description
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.title
import com.x8bit.bitwarden.ui.platform.composition.LocalAuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.composition.LocalNfcManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import kotlinx.collections.immutable.toPersistentList

/**
 * The top level composable for the Two-Factor Login screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorLoginScreen(
    onNavigateBack: () -> Unit,
    viewModel: TwoFactorLoginViewModel = hiltViewModel(),
    authTabLaunchers: AuthTabLaunchers = LocalAuthTabLaunchers.current,
    intentManager: IntentManager = LocalIntentManager.current,
    nfcManager: NfcManager = LocalNfcManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (state.shouldListenForNfc) {
                    nfcManager.start()
                }
            }

            Lifecycle.Event.ON_PAUSE -> {
                if (state.shouldListenForNfc) {
                    nfcManager.stop()
                }
            }

            else -> Unit
        }
    }
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            TwoFactorLoginEvent.NavigateBack -> onNavigateBack()

            is TwoFactorLoginEvent.NavigateToRecoveryCode -> {
                intentManager.launchUri(uri = event.uri)
            }

            is TwoFactorLoginEvent.NavigateToDuo -> {
                intentManager.startAuthTab(
                    uri = event.uri,
                    redirectScheme = event.scheme,
                    launcher = authTabLaunchers.duo,
                )
            }

            is TwoFactorLoginEvent.NavigateToWebAuth -> {
                intentManager.startAuthTab(
                    uri = event.uri,
                    redirectScheme = event.scheme,
                    launcher = authTabLaunchers.webAuthn,
                )
            }

            is TwoFactorLoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    TwoFactorLoginDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = if (state.isNewDeviceVerification) {
                    stringResource(id = BitwardenString.verify_your_identity)
                } else {
                    state.authMethod.title()
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(TwoFactorLoginAction.CloseButtonClick) }
                },
                actions = {
                    if (!state.isNewDeviceVerification) {
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(BitwardenString.more),
                            menuItemDataList = state.availableAuthMethods
                                .map {
                                    OverflowMenuItemData(
                                        text = it.title(),
                                        onClick = remember(viewModel) {
                                            {
                                                viewModel.trySendAction(
                                                    TwoFactorLoginAction.SelectAuthMethod(it),
                                                )
                                            }
                                        },
                                    )
                                }
                                .toPersistentList(),
                        )
                    }
                },
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        TwoFactorLoginScreenContent(
            state = state,
            onCodeInputChange = remember(viewModel) {
                { viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(it)) }
            },
            onContinueButtonClick = remember(viewModel) {
                { viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick) }
            },
            onRememberMeToggle = remember(viewModel) {
                { viewModel.trySendAction(TwoFactorLoginAction.RememberMeToggle(it)) }
            },
            onResendEmailButtonClick = remember(viewModel) {
                { viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TwoFactorLoginDialogs(
    dialogState: TwoFactorLoginState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is TwoFactorLoginState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState
                .title
                ?.invoke()
                ?: stringResource(BitwardenString.an_error_has_occurred),
            message = dialogState.message(),
            throwable = dialogState.error,
            onDismissRequest = onDismissRequest,
        )

        is TwoFactorLoginState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun TwoFactorLoginScreenContent(
    state: TwoFactorLoginState,
    onCodeInputChange: (String) -> Unit,
    onContinueButtonClick: () -> Unit,
    onRememberMeToggle: (Boolean) -> Unit,
    onResendEmailButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        state.imageRes?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .size(124.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isNewDeviceVerification) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            Text(
                text = stringResource(id = BitwardenString.enter_verification_code_new_device),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        } else {
            state.authMethod.description(email = state.displayEmail)?.let { text ->
                Spacer(modifier = Modifier.height(height = 12.dp))
                Text(
                    text = text(),
                    textAlign = TextAlign.Center,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (state.shouldShowCodeInput) {
            BitwardenTextField(
                value = state.codeInput,
                onValueChange = onCodeInputChange,
                label = stringResource(id = BitwardenString.verification_code),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                autoFocus = true,
                keyboardActions = KeyboardActions(
                    onDone = { onContinueButtonClick() },
                ),
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        if (!state.isNewDeviceVerification) {
            BitwardenSwitch(
                label = stringResource(id = BitwardenString.remember),
                isChecked = state.isRememberEnabled,
                onCheckedChange = onRememberMeToggle,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = state.buttonText(),
            onClick = onContinueButtonClick,
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        if (state.authMethod == TwoFactorAuthMethod.EMAIL) {
            Spacer(modifier = Modifier.height(12.dp))

            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.send_verification_code_again),
                onClick = onResendEmailButtonClick,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
@Preview(showBackground = true)
private fun TwoFactorLoginScreenContentPreview() {
    BitwardenTheme {
        TwoFactorLoginScreenContent(
            state = TwoFactorLoginState(
                authMethod = TwoFactorAuthMethod.EMAIL,
                availableAuthMethods = listOf(TwoFactorAuthMethod.EMAIL),
                codeInput = "",
                dialogState = null,
                displayEmail = "email@dot.com",
                isContinueButtonEnabled = true,
                isRememberEnabled = true,
                email = "",
                password = "",
                orgIdentifier = null,
                isNewDeviceVerification = true,
            ),
            onCodeInputChange = {},
            onContinueButtonClick = {},
            onRememberMeToggle = {},
            onResendEmailButtonClick = {},
        )
    }
}
