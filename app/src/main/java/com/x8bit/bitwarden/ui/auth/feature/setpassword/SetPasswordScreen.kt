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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenPolicyWarningText

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
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.set_master_password),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.cancel),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SetPasswordAction.CancelClick) }
                        },
                        modifier = Modifier.semantics { testTag = "CancelButton" },
                    )
                    BitwardenTextButton(
                        label = stringResource(id = R.string.submit),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SetPasswordAction.SubmitClick) }
                        },
                        modifier = Modifier.semantics { testTag = "SubmitButton" },
                    )
                },
            )
        },
    ) { innerPadding ->
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
                .padding(innerPadding)
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenPolicyWarningText(
            text = stringResource(id = R.string.reset_password_auto_enroll_invite_warning),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            value = state.passwordInput,
            onValueChange = onPasswordInputChanged,
            hint = stringResource(id = R.string.master_password_description),
            modifier = Modifier
                .semantics { testTag = "NewPasswordField" }
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenPasswordField(
            label = stringResource(id = R.string.retype_master_password),
            value = state.retypePasswordInput,
            onValueChange = onRetypePasswordInputChanged,
            modifier = Modifier
                .semantics { testTag = "RetypePasswordField" }
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
                .semantics { testTag = "MasterPasswordHintLabel" }
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is SetPasswordState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialogState.message,
                ),
            )
        }

        null -> Unit
    }
}
