package com.x8bit.bitwarden.ui.auth.feature.setpassword

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the Set Master Password screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPasswordScreen(
    viewModel: SetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    SetPasswordDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(SetPasswordAction.DialogDismiss) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.set_master_password),
                navigationIcon = null,
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.cancel),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SetPasswordAction.CancelClick) }
                        },
                        modifier = Modifier.testTag("CancelButton"),
                    )
                    BitwardenTextButton(
                        label = stringResource(id = R.string.submit),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SetPasswordAction.SubmitClick) }
                        },
                        modifier = Modifier.testTag("SubmitButton"),
                    )
                },
            )
        },
    ) {
        SetPasswordScreenContent(
            state = state,
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(SetPasswordAction.PasswordInputChanged(it)) }
            },
            onRetypePasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged(it)) }
            },
            onPasswordHintInputChanged = remember(viewModel) {
                { viewModel.trySendAction(SetPasswordAction.PasswordHintInputChanged(it)) }
            },
            modifier = Modifier
                .imePadding()
                .fillMaxSize(),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun SetPasswordScreenContent(
    state: SetPasswordState,
    onPasswordInputChanged: (String) -> Unit,
    onRetypePasswordInputChanged: (String) -> Unit,
    onPasswordHintInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(
                id = R.string.your_organization_requires_you_to_set_a_master_password,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenInfoCalloutCard(
            text = stringResource(id = R.string.reset_password_auto_enroll_invite_warning),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            value = state.passwordInput,
            onValueChange = onPasswordInputChanged,
            showPassword = isPasswordVisible,
            showPasswordChange = { isPasswordVisible = it },
            hint = stringResource(id = R.string.master_password_description),
            modifier = Modifier
                .testTag("NewPasswordField")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenPasswordField(
            label = stringResource(id = R.string.retype_master_password),
            value = state.retypePasswordInput,
            onValueChange = onRetypePasswordInputChanged,
            showPassword = isPasswordVisible,
            showPasswordChange = { isPasswordVisible = it },
            modifier = Modifier
                .testTag("RetypePasswordField")
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenTextField(
            label = stringResource(id = R.string.master_password_hint),
            value = state.passwordHintInput,
            onValueChange = onPasswordHintInputChanged,
            hint = stringResource(id = R.string.master_password_hint_description),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textFieldTestTag = "MasterPasswordHintLabel",
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SetPasswordDialogs(
    dialogState: SetPasswordState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is SetPasswordState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is SetPasswordState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        null -> Unit
    }
}
