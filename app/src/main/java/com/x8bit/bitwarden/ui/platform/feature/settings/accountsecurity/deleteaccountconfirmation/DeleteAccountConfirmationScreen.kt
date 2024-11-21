package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledErrorButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the delete account confirmation screen.
 */
@Composable
fun DeleteAccountConfirmationScreen(
    viewModel: DeleteAccountConfirmationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            DeleteAccountConfirmationEvent.NavigateBack -> onNavigateBack()

            is DeleteAccountConfirmationEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DeleteAccountConfirmationDialogs(
        dialogState = state.dialog,
        onDeleteAccountAcknowledge = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.DeleteAccountAcknowledge) }
        },
        onDismissDialog = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.DismissDialog) }
        },
    )

    DeleteAccountConfirmationScaffold(
        state = state,
        onCloseClick = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.CloseClick) }
        },
        onDeleteAccountClick = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.DeleteAccountClick) }
        },
        onResendCodeClick = remember(viewModel) {
            { viewModel.trySendAction(DeleteAccountConfirmationAction.ResendCodeClick) }
        },
        onVerificationCodeTextChange = remember(viewModel) {
            {
                viewModel.trySendAction(
                    DeleteAccountConfirmationAction.VerificationCodeTextChange(it),
                )
            }
        },
    )
}

@Composable
private fun DeleteAccountConfirmationDialogs(
    dialogState: DeleteAccountConfirmationState.DeleteAccountConfirmationDialog?,
    onDismissDialog: () -> Unit,
    onDeleteAccountAcknowledge: () -> Unit,
) {
    when (dialogState) {
        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess -> {
            BitwardenBasicDialog(
                title = null,
                message = dialogState.message(),
                onDismissRequest = onDeleteAccountAcknowledge,
            )
        }

        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                onDismissRequest = onDismissDialog,
            )
        }

        is DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading -> {
            BitwardenLoadingDialog(text = dialogState.title())
        }

        null -> Unit
    }
}

@Composable
private fun DeleteAccountConfirmationContent(
    state: DeleteAccountConfirmationState,
    onDeleteAccountClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onVerificationCodeTextChange: (verificationCode: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(id = R.string.a_verification_code_was_sent_to_your_email),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenPasswordField(
            value = state.verificationCode,
            onValueChange = onVerificationCodeTextChange,
            label = stringResource(id = R.string.verification_code),
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            autoFocus = true,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.confirm_your_identity),
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenFilledErrorButton(
            label = stringResource(id = R.string.delete_account),
            onClick = onDeleteAccountClick,
            isEnabled = state.verificationCode.isNotBlank(),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(id = R.string.resend_code),
            onClick = onResendCodeClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountConfirmationScaffold(
    state: DeleteAccountConfirmationState,
    onCloseClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onVerificationCodeTextChange: (verificationCode: String) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.verification_code),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = onCloseClick,
            )
        },
    ) {
        DeleteAccountConfirmationContent(
            state = state,
            onDeleteAccountClick = onDeleteAccountClick,
            onResendCodeClick = onResendCodeClick,
            onVerificationCodeTextChange = onVerificationCodeTextChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteAccountConfirmationScreen_preview() {
    BitwardenTheme {
        DeleteAccountConfirmationScaffold(
            state = DeleteAccountConfirmationState(
                dialog = null,
                verificationCode = "123456",
            ),
            onCloseClick = {},
            onDeleteAccountClick = {},
            onResendCodeClick = {},
            onVerificationCodeTextChange = {},
        )
    }
}
