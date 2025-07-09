package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.description
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.title
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.x8bit.bitwarden.ui.platform.components.snackbar.rememberBitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.composition.LocalNfcManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
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

            is TwoFactorLoginEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is TwoFactorLoginEvent.NavigateToDuo -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is TwoFactorLoginEvent.NavigateToWebAuth -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val title = if (state.isNewDeviceVerification) {
        R.string.verify_your_identity.asText()
    } else {
        state.authMethod.title
    }
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = title(),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(TwoFactorLoginAction.CloseButtonClick) }
                },
                actions = {
                    if (!state.isNewDeviceVerification) {
                        BitwardenOverflowActionItem(
                            contentDescription = stringResource(R.string.more),
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
                ?: stringResource(R.string.an_error_has_occurred),
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
        if (state.authMethod != TwoFactorAuthMethod.YUBI_KEY) {
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
        }
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = if (state.isNewDeviceVerification) {
                stringResource(R.string.enter_verification_code_new_device)
            } else {
                state.authMethod.description(
                    state.displayEmail,
                ).invoke()
            },
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.authMethod == TwoFactorAuthMethod.YUBI_KEY) {
            state.imageRes?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (state.shouldShowCodeInput) {
            BitwardenPasswordField(
                value = state.codeInput,
                onValueChange = onCodeInputChange,
                label = stringResource(id = R.string.verification_code),
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
                label = stringResource(id = R.string.remember),
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
                label = stringResource(id = R.string.send_verification_code_again),
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
                captchaToken = null,
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
