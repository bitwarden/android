package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * The top level composable for the Login screen.
 */
@Composable
@Suppress("LongMethod")
fun LoginScreen(
    onNavigateToLanding: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LoginEvent.NavigateToLanding -> onNavigateToLanding()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 32.dp),
    ) {

        BitwardenTextField(
            modifier = Modifier.testTag("Master password"),
            value = state.passwordInput,
            onValueChange = { viewModel.trySendAction(LoginAction.PasswordInputChanged(it)) },
            label = stringResource(id = R.string.master_password),
        )

        Button(
            onClick = { viewModel.trySendAction(LoginAction.LoginButtonClick) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("Login button"),
            enabled = state.isLoginButtonEnabled,
        ) {
            Text(
                text = stringResource(id = R.string.log_in_with_master_password),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = { viewModel.trySendAction(LoginAction.SingleSignOnClick) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("Single sign-on button"),
            enabled = state.isLoginButtonEnabled,
        ) {
            Text(
                text = stringResource(id = R.string.log_in_sso),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        // TODO Get the "login target" from a dropdown (BIT-202)
        Text(
            text = stringResource(id = R.string.log_in_attempt_by_x_on_y, state.emailAddress, "bitwarden.com"),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            modifier = Modifier
                .clickable { viewModel.trySendAction(LoginAction.NotYouButtonClick) },
            text = stringResource(id = R.string.not_you),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
