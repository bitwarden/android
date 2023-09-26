package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * Top level composable for the create account screen.
 */
@Suppress("LongMethod")
@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is CreateAccountEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                text = stringResource(id = R.string.create_account),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier
                    .clickable {
                        viewModel.trySendAction(SubmitClick)
                    }
                    .padding(16.dp),
                text = stringResource(id = R.string.submit),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BitwardenTextField(
            label = stringResource(id = R.string.email_address),
            value = state.emailInput,
            onValueChange = { viewModel.trySendAction(EmailInputChange(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        BitwardenTextField(
            label = stringResource(id = R.string.master_password),
            value = state.passwordInput,
            onValueChange = { viewModel.trySendAction(PasswordInputChange(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        BitwardenTextField(
            label = stringResource(id = R.string.retype_master_password),
            value = state.confirmPasswordInput,
            onValueChange = { viewModel.trySendAction(ConfirmPasswordInputChange(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        BitwardenTextField(
            label = stringResource(id = R.string.master_password_hint),
            value = state.passwordHintInput,
            onValueChange = { viewModel.trySendAction(PasswordHintChange(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
