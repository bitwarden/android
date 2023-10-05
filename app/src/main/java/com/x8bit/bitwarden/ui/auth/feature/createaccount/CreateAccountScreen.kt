package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButtonTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField

/**
 * Top level composable for the create account screen.
 */
@Suppress("LongMethod")
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    EventsEffect(viewModel) { event ->
        when (event) {
            is CreateAccountEvent.NavigateBack -> onNavigateBack.invoke()
            is CreateAccountEvent.ShowToast -> {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    BitwardenBasicDialog(
        visibilityState = state.errorDialogState,
        onDismissRequest = remember(viewModel) { { viewModel.trySendAction(ErrorDialogDismiss) } },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        BitwardenTextButtonTopAppBar(
            title = stringResource(id = R.string.create_account),
            navigationIcon = painterResource(id = R.drawable.ic_close),
            navigationIconContentDescription = stringResource(id = R.string.close),
            onNavigationIconClick = remember(viewModel) {
                { viewModel.trySendAction(CreateAccountAction.CloseClick) }
            },
            buttonText = stringResource(id = R.string.submit),
            onButtonClick = remember(viewModel) {
                { viewModel.trySendAction(CreateAccountAction.SubmitClick) }
            },
            isButtonEnabled = true,
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = spacedBy(16.dp),
        ) {
            BitwardenTextField(
                label = stringResource(id = R.string.email_address),
                value = state.emailInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(EmailInputChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenPasswordField(
                label = stringResource(id = R.string.master_password),
                value = state.passwordInput,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(PasswordInputChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenPasswordField(
                label = stringResource(id = R.string.retype_master_password),
                value = state.confirmPasswordInput,
                onValueChange = remember {
                    { viewModel.trySendAction(ConfirmPasswordInputChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenTextField(
                label = stringResource(id = R.string.master_password_hint),
                value = state.passwordHintInput,
                onValueChange = remember { { viewModel.trySendAction(PasswordHintChange(it)) } },
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenFilledButton(
                label = stringResource(id = R.string.submit),
                onClick = remember { { viewModel.trySendAction(CreateAccountAction.SubmitClick) } },
                isEnabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
