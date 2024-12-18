package com.x8bit.bitwarden.ui.auth.feature.resetpassword

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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold

/**
 * The top level composable for the Reset Password screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    when (val dialog = state.dialogState) {
        is ResetPasswordState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(ResetPasswordAction.DialogDismiss) }
                },
            )
        }

        is ResetPasswordState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }

    var shouldShowLogoutConfirmationDialog by remember { mutableStateOf(false) }
    val onLogoutClicked = remember(viewModel) {
        { viewModel.trySendAction(ResetPasswordAction.ConfirmLogoutClick) }
    }
    if (shouldShowLogoutConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.log_out),
            message = stringResource(id = R.string.logout_confirmation),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                shouldShowLogoutConfirmationDialog = false
                onLogoutClicked()
            },
            onDismissClick = { shouldShowLogoutConfirmationDialog = false },
            onDismissRequest = { shouldShowLogoutConfirmationDialog = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.update_master_password),
                navigationIcon = null,
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.log_out),
                        onClick = { shouldShowLogoutConfirmationDialog = true },
                        modifier = Modifier.testTag("LogoutButton"),
                    )
                    BitwardenTextButton(
                        label = stringResource(id = R.string.submit),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(ResetPasswordAction.SubmitClick) }
                        },
                        modifier = Modifier.testTag("SubmitButton"),
                    )
                },
            )
        },
    ) {
        ResetPasswordScreenContent(
            state = state,
            onCurrentPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(it)) }
            },
            onPasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(it)) }
            },
            onRetypePasswordInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(it)) }
            },
            onPasswordHintInputChanged = remember(viewModel) {
                { viewModel.trySendAction(ResetPasswordAction.PasswordHintInputChanged(it)) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun ResetPasswordScreenContent(
    state: ResetPasswordState,
    onCurrentPasswordInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onRetypePasswordInputChanged: (String) -> Unit,
    onPasswordHintInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {

        val instructionsTextId =
            if (state.resetReason == ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN) {
                R.string.update_weak_master_password_warning
            } else {
                R.string.update_master_password_warning
            }
        BitwardenInfoCalloutCard(
            text = stringResource(id = instructionsTextId),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.resetReason == ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN) {
            val passwordPolicyContent = listOf(
                stringResource(id = R.string.master_password_policy_in_effect),
            )
                .plus(state.policies.map { it() })
                .joinToString("\n  •  ")
            BitwardenInfoCalloutCard(
                text = passwordPolicyContent,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenPasswordField(
                label = stringResource(id = R.string.current_master_password),
                value = state.currentPasswordInput,
                onValueChange = onCurrentPasswordInputChanged,
                modifier = Modifier
                    .testTag("MasterPasswordField")
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            value = state.passwordInput,
            onValueChange = onPasswordInputChanged,
            showPassword = isPasswordVisible,
            showPasswordChange = { isPasswordVisible = it },
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
