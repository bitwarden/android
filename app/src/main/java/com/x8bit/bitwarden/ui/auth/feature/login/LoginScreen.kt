package com.x8bit.bitwarden.ui.auth.feature.login

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.account.BitwardenPlaceholderAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMasterPasswordHint: (String) -> Unit,
    onNavigateToEnterpriseSignOn: (String) -> Unit,
    onNavigateToLoginWithDevice: (emailAddress: String) -> Unit,
    onNavigateToTwoFactorLogin: (String, String?) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginEvent.NavigateBack -> onNavigateBack()
            is LoginEvent.NavigateToMasterPasswordHint -> {
                onNavigateToMasterPasswordHint(event.emailAddress)
            }

            is LoginEvent.NavigateToCaptcha -> {
                intentManager.startCustomTabsActivity(uri = event.uri)
            }

            is LoginEvent.NavigateToEnterpriseSignOn -> {
                onNavigateToEnterpriseSignOn(event.emailAddress)
            }

            is LoginEvent.NavigateToLoginWithDevice -> {
                onNavigateToLoginWithDevice(event.emailAddress)
            }

            is LoginEvent.NavigateToTwoFactorLogin -> {
                onNavigateToTwoFactorLogin(event.emailAddress, event.password)
            }

            is LoginEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LoginDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(LoginAction.ErrorDialogDismiss) }
        },
    )

    val isAccountButtonVisible = state.accountSummaries.isNotEmpty()
    var isAccountMenuVisible by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.app_name),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginAction.CloseButtonClick) }
                },
                actions = {
                    if (isAccountButtonVisible) {
                        BitwardenPlaceholderAccountActionItem(
                            onClick = { isAccountMenuVisible = !isAccountMenuVisible },
                        )
                    }
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.get_password_hint),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(LoginAction.MasterPasswordHintClick) }
                                },
                            ),
                        ),
                    )
                },
            )
        },
        overlay = {
            BitwardenAccountSwitcher(
                isVisible = isAccountMenuVisible,
                accountSummaries = state.accountSummaries.toImmutableList(),
                onSwitchAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginAction.SwitchAccountClick(it)) }
                },
                onLockAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginAction.LockAccountClick(it)) }
                },
                onLogoutAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginAction.LogoutAccountClick(it)) }
                },
                onAddAccountClick = remember(viewModel) {
                    { viewModel.trySendAction(LoginAction.AddAccountClick) }
                },
                onDismissRequest = { isAccountMenuVisible = false },
                topAppBarScrollBehavior = scrollBehavior,
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        LoginScreenContent(
            state = state,
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.PasswordInputChanged(it)) }
            },
            onMasterPasswordClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.MasterPasswordHintClick) }
            },
            onLoginButtonClick = remember(viewModel) {
                {
                    keyboardController?.hide()
                    viewModel.trySendAction(LoginAction.LoginButtonClick)
                }
            },
            onLoginWithDeviceClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.LoginWithDeviceButtonClick) }
            },
            onSingleSignOnClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.SingleSignOnClick) }
            },
            onNotYouButtonClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.NotYouButtonClick) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LoginDialogs(
    dialogState: LoginState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is LoginState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
        )

        is LoginState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun LoginScreenContent(
    state: LoginState,
    onPasswordInputChanged: (String) -> Unit,
    onMasterPasswordClick: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onLoginWithDeviceClick: () -> Unit,
    onSingleSignOnClick: () -> Unit,
    onNotYouButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        BitwardenPasswordField(
            autoFocus = true,
            modifier = Modifier
                .testTag("MasterPasswordEntry")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = state.passwordInput,
            onValueChange = onPasswordInputChanged,
            label = stringResource(id = R.string.master_password),
            showPasswordTestTag = "PasswordVisibilityToggle",
        )

        BitwardenClickableText(
            label = stringResource(id = R.string.get_master_passwordword_hint),
            onClick = onMasterPasswordClick,
            style = BitwardenTheme.typography.bodySmall,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("GetMasterPasswordHintLabel"),
        )

        Spacer(modifier = Modifier.height(20.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.log_in_with_master_password),
            onClick = onLoginButtonClick,
            isEnabled = state.isLoginButtonEnabled,
            modifier = Modifier
                .testTag("LogInWithMasterPasswordButton")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.shouldShowLoginWithDevice) {
            BitwardenOutlinedButton(
                label = stringResource(id = R.string.log_in_with_device),
                icon = rememberVectorPainter(id = R.drawable.ic_mobile_small),
                onClick = onLoginWithDeviceClick,
                modifier = Modifier
                    .testTag("LogInWithAnotherDeviceButton")
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        BitwardenOutlinedButton(
            label = stringResource(id = R.string.log_in_sso),
            icon = rememberVectorPainter(id = R.drawable.ic_enterprise_small),
            onClick = onSingleSignOnClick,
            modifier = Modifier
                .testTag("LogInWithSsoButton")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(
                id = R.string.logging_in_as_x_on_y,
                state.emailAddress,
                state.environmentLabel,
            ),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .testTag("LoggingInAsLabel")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        BitwardenClickableText(
            label = stringResource(id = R.string.not_you),
            onClick = onNotYouButtonClick,
            style = BitwardenTheme.typography.labelLarge,
            innerPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            modifier = Modifier.testTag("NotYouLabel"),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
