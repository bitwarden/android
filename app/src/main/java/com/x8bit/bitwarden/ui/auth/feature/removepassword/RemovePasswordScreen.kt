package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The top level composable for the Remove Password screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemovePasswordScreen(
    viewModel: RemovePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    RemovePasswordDialogs(
        dialogState = state.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(RemovePasswordAction.DialogDismiss) }
        },
        onConfirmLeaveClick = remember(viewModel) {
            {
                viewModel.trySendAction(RemovePasswordAction.ConfirmLeaveOrganizationClick)
            }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.remove_master_password),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
            )
        },
    ) {
        RemovePasswordScreenContent(
            state = state,
            onContinueClick = remember(viewModel) {
                { viewModel.trySendAction(RemovePasswordAction.ContinueClick) }
            },
            onInputChanged = remember(viewModel) {
                { viewModel.trySendAction(RemovePasswordAction.InputChanged(it)) }
            },
            onLeaveOrganizationClick = remember(viewModel) {
                { viewModel.trySendAction(RemovePasswordAction.LeaveOrganizationClick) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun RemovePasswordScreenContent(
    state: RemovePasswordState,
    onContinueClick: () -> Unit,
    onInputChanged: (String) -> Unit,
    onLeaveOrganizationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = state.description(),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = state.labelOrg(),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Text(
            text = state.orgName?.invoke().orEmpty(),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = state.labelDomain(),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Text(
            text = state.domainName?.invoke().orEmpty(),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        BitwardenPasswordField(
            label = stringResource(id = R.string.master_password),
            value = state.input,
            onValueChange = onInputChanged,
            showPasswordTestTag = "PasswordVisibilityToggle",
            passwordFieldTestTag = "MasterPasswordEntry",
            autoFocus = true,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = R.string.continue_text),
            onClick = onContinueClick,
            isEnabled = state.input.isNotEmpty(),
            modifier = Modifier
                .testTag(tag = "ContinueButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = R.string.leave_organization),
            onClick = onLeaveOrganizationClick,
            modifier = Modifier
                .testTag("LeaveOrganizationButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun RemovePasswordDialogs(
    dialogState: RemovePasswordState.DialogState?,
    onDismissRequest: () -> Unit,
    onConfirmLeaveClick: () -> Unit,
) {
    when (dialogState) {
        is RemovePasswordState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke(),
                message = dialogState.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is RemovePasswordState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.title())
        }

        is RemovePasswordState.DialogState.LeaveConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.leave_organization),
                message = dialogState.message.invoke(),
                confirmButtonText = stringResource(id = R.string.confirm),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = onConfirmLeaveClick,
                onDismissClick = onDismissRequest,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun RemovePasswordScreen_preview() {
    BitwardenTheme {
        RemovePasswordScreenContent(
            state = RemovePasswordState(
                input = "",
                description =
                    ("A master password is no longer required " +
                        "for members of the following organization. " +
                        "Please confirm the domain below with your " +
                        "organization administrator.").asText(),
                labelOrg = "Organization name".asText(),
                orgName = "Organization name".asText(),
                labelDomain = "Key Connector domain".asText(),
                domainName = "http://localhost:8080".asText(),
                dialogState = null,
                organizationId = null,
            ),
            onContinueClick = { },
            onInputChanged = { },
            onLeaveOrganizationClick = { },
        )
    }
}
