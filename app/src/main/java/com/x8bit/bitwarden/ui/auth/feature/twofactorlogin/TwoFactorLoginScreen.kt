package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.description
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.title
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
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
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            TwoFactorLoginEvent.NavigateBack -> onNavigateBack()

            TwoFactorLoginEvent.NavigateToRecoveryCode -> {
                intentManager.launchUri("https://bitwarden.com/help/lost-two-step-device".toUri())
            }

            is TwoFactorLoginEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is TwoFactorLoginEvent.ShowToast -> {
                Toast.makeText(context, event.message(context.resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val dialog = state.dialogState) {
        is TwoFactorLoginState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialog.title ?: R.string.an_error_has_occurred.asText(),
                    message = dialog.message,
                ),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(TwoFactorLoginAction.DialogDismiss) }
                },
            )
        }

        is TwoFactorLoginState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialog.message,
                ),
            )
        }

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.authMethod.title(),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(TwoFactorLoginAction.CloseButtonClick) }
                },
                actions = {
                    BitwardenOverflowActionItem(
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
                },
            )
        },
    ) { innerPadding ->
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
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
            .semantics { testTagsAsResourceId = true }
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = state.authMethod.description(state.displayEmail)(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenTextField(
            value = state.codeInput,
            onValueChange = onCodeInputChange,
            label = stringResource(id = R.string.verification_code),
            keyboardType = KeyboardType.Number,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenSwitch(
            label = stringResource(id = R.string.remember_me),
            isChecked = state.isRememberMeEnabled,
            onCheckedChange = onRememberMeToggle,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = onContinueButtonClick,
            isEnabled = state.isContinueButtonEnabled,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        if (state.authMethod == TwoFactorAuthMethod.EMAIL) {
            Spacer(modifier = Modifier.height(12.dp))

            BitwardenFilledTonalButton(
                label = stringResource(id = R.string.send_verification_code_again),
                onClick = onResendEmailButtonClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
