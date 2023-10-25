package com.x8bit.bitwarden.ui.auth.feature.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOutlinedButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar

/**
 * The top level composable for the Login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun LoginScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    intentHandler: IntentHandler = IntentHandler(context = LocalContext.current),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginEvent.NavigateBack -> onNavigateBack()
            is LoginEvent.NavigateToCaptcha -> {
                intentHandler.startCustomTabsActivity(uri = event.uri)
            }

            is LoginEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
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
                    BitwardenOverflowActionItem(
                        dropdownMenuItemContent = {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.get_password_hint))
                                },
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(LoginAction.MasterPasswordHintClick) }
                                },
                            )
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
        ) {
            BitwardenLoadingDialog(
                visibilityState = state.loadingDialogState,
            )
            BitwardenBasicDialog(
                visibilityState = state.errorDialogState,
                onDismissRequest = { viewModel.trySendAction(LoginAction.ErrorDialogDismiss) },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                BitwardenPasswordField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = state.passwordInput,
                    onValueChange = remember(viewModel) {
                        { viewModel.trySendAction(LoginAction.PasswordInputChanged(it)) }
                    },
                    label = stringResource(id = R.string.master_password),
                )

                // TODO: Need to figure out better handling for very small clickable text (BIT-724)
                Text(
                    text = stringResource(id = R.string.get_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            viewModel.trySendAction(LoginAction.MasterPasswordHintClick)
                        }
                        .padding(
                            vertical = 4.dp,
                            horizontal = 16.dp,
                        ),
                )

                BitwardenFilledButton(
                    label = stringResource(id = R.string.log_in_with_master_password),
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(LoginAction.LoginButtonClick) }
                    },
                    isEnabled = state.isLoginButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )

                BitwardenOutlinedButtonWithIcon(
                    label = stringResource(id = R.string.log_in_sso),
                    icon = painterResource(id = R.drawable.ic_light_bulb),
                    onClick =
                    remember(viewModel) {
                        { viewModel.trySendAction(LoginAction.SingleSignOnClick) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    isEnabled = state.isLoginButtonEnabled,
                )

                Text(
                    text = stringResource(
                        id = R.string.logging_in_as_x_on_y,
                        state.emailAddress,
                        state.environmentLabel(),
                    ),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                // TODO: Need to figure out better handling for very small clickable text (BIT-724)
                Text(
                    modifier = Modifier
                        .clickable { viewModel.trySendAction(LoginAction.NotYouButtonClick) },
                    text = stringResource(id = R.string.not_you),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
