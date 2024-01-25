package com.x8bit.bitwarden.ui.auth.feature.login

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenAccountSwitcher
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOutlinedButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenPlaceholderAccountActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level composable for the Login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMasterPasswordHint: (String) -> Unit,
    onNavigateToEnterpriseSignOn: () -> Unit,
    onNavigateToLoginWithDevice: (emailAddress: String) -> Unit,
    onNavigateToTwoFactorLogin: (String, String?) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
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

            LoginEvent.NavigateToEnterpriseSignOn -> onNavigateToEnterpriseSignOn()
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
                navigationIcon = painterResource(id = R.drawable.ic_close),
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
    ) { innerPadding ->
        LoginScreenContent(
            state = state,
            onErrorDialogDismiss = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.ErrorDialogDismiss) }
            },
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.PasswordInputChanged(it)) }
            },
            onMasterPasswordClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.MasterPasswordHintClick) }
            },
            onLoginButtonClick = remember(viewModel) {
                { viewModel.trySendAction(LoginAction.LoginButtonClick) }
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )

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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginScreenContent(
    state: LoginState,
    onErrorDialogDismiss: () -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onMasterPasswordClick: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onLoginWithDeviceClick: () -> Unit,
    onSingleSignOnClick: () -> Unit,
    onNotYouButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        BitwardenLoadingDialog(
            visibilityState = state.loadingDialogState,
        )
        BitwardenBasicDialog(
            visibilityState = state.errorDialogState,
            onDismissRequest = onErrorDialogDismiss,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            BitwardenPasswordField(
                modifier = Modifier
                    .semantics { testTag = "MasterPasswordEntry" }
                    .fillMaxWidth(),
                value = state.passwordInput,
                onValueChange = onPasswordInputChanged,
                label = stringResource(id = R.string.master_password),
                showPasswordTestTag = "PasswordVisibilityToggle",
            )

            // TODO: Need to figure out better handling for very small clickable text (BIT-724)
            Text(
                text = stringResource(id = R.string.get_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { testTag = "GetMasterPasswordHintLabel" }
                    .fillMaxWidth()
                    .clickable { onMasterPasswordClick() }
                    .padding(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                    ),
            )

            Spacer(modifier = Modifier.height(20.dp))

            BitwardenFilledButton(
                label = stringResource(id = R.string.log_in_with_master_password),
                onClick = onLoginButtonClick,
                isEnabled = state.isLoginButtonEnabled,
                modifier = Modifier
                    .semantics { testTag = "LogInWithMasterPasswordButton" }
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.shouldShowLoginWithDevice) {
                BitwardenOutlinedButtonWithIcon(
                    label = stringResource(id = R.string.log_in_with_device),
                    icon = painterResource(id = R.drawable.ic_device),
                    onClick = onLoginWithDeviceClick,
                    modifier = Modifier
                        .semantics { testTag = "LogInWithAnotherDeviceButton" }
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            BitwardenOutlinedButtonWithIcon(
                label = stringResource(id = R.string.log_in_sso),
                icon = painterResource(id = R.drawable.ic_briefcase),
                onClick = onSingleSignOnClick,
                modifier = Modifier
                    .semantics { testTag = "LogInWithSsoButton" }
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .semantics { testTag = "LoggingInAsLabel" }
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenClickableText(
                modifier = Modifier
                    .semantics { testTag = "NotYouLabel" },
                onClick = onNotYouButtonClick,
                label = stringResource(id = R.string.not_you),
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
