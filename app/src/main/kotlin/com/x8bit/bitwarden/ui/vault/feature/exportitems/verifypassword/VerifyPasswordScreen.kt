package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeCompletionManager
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeRequestValidator
import com.bitwarden.cxf.validator.CredentialExchangeRequestValidator
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.exportitems.component.AccountSummaryListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.component.ExportItemsScaffold
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.handlers.VerifyPasswordHandlers
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.handlers.rememberVerifyPasswordHandler

/**
 * Top level composable for the Verify Password screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VerifyPasswordScreen(
    onNavigateBack: () -> Unit,
    onPasswordVerified: (userId: String) -> Unit,
    viewModel: VerifyPasswordViewModel = hiltViewModel(),
    credentialExchangeCompletionManager: CredentialExchangeCompletionManager =
        LocalCredentialExchangeCompletionManager.current,
    credentialExchangeRequestValidator: CredentialExchangeRequestValidator =
        LocalCredentialExchangeRequestValidator.current,
    snackbarHostState: BitwardenSnackbarHostState = rememberBitwardenSnackbarHostState(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val handler = rememberVerifyPasswordHandler(viewModel)

    EventsEffect(viewModel) { event ->
        when (event) {
            VerifyPasswordEvent.NavigateBack -> onNavigateBack()
            VerifyPasswordEvent.CancelExport -> {
                credentialExchangeCompletionManager
                    .completeCredentialExport(
                        exportResult = ExportCredentialsResult.Failure(
                            error = ImportCredentialsCancellationException(
                                errorMessage = "User cancelled import.",
                            ),
                        ),
                    )
            }

            is VerifyPasswordEvent.ValidateImportRequest -> {
                viewModel.trySendAction(
                    VerifyPasswordAction.ValidateImportRequestResultReceive(
                        isValid = credentialExchangeRequestValidator.validate(
                            importCredentialsRequestData = event.importCredentialsRequestData,
                        ),
                    ),
                )
            }

            is VerifyPasswordEvent.PasswordVerified -> {
                onPasswordVerified(event.userId)
            }

            is VerifyPasswordEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(event.data)
            }
        }
    }

    VerifyPasswordDialogs(
        dialog = state.dialog,
        onDismiss = handler.onDismissDialog,
    )

    ExportItemsScaffold(
        navIcon = rememberVectorPainter(
            id = if (state.hasOtherAccounts) {
                BitwardenDrawable.ic_back
            } else {
                BitwardenDrawable.ic_close
            },
        ),
        onNavigationIconClick = handler.onNavigateBackClick,
        navigationIconContentDescription = stringResource(BitwardenString.back),
        scrollBehavior = scrollBehavior,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val viewState = state.viewState) {
            is VerifyPasswordState.ViewState.Content -> {
                VerifyPasswordContent(
                    viewState = viewState,
                    accountSummaryListItem = state.accountSummaryListItem,
                    handler = handler,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VerifyPasswordState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            VerifyPasswordState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    text = stringResource(id = BitwardenString.loading),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun VerifyPasswordDialogs(
    dialog: VerifyPasswordState.DialogState?,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is VerifyPasswordState.DialogState.General -> {
            BitwardenBasicDialog(
                title = dialog.title(),
                message = dialog.message(),
                throwable = dialog.error,
                onDismissRequest = onDismiss,
            )
        }

        is VerifyPasswordState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun VerifyPasswordContent(
    viewState: VerifyPasswordState.ViewState.Content,
    accountSummaryListItem: AccountSelectionListItem,
    handler: VerifyPasswordHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = viewState.title(),
            textAlign = TextAlign.Center,
            style = BitwardenTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        viewState.subtext?.let { subtext ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtext(),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        Spacer(Modifier.height(16.dp))

        AccountSummaryListItem(
            item = accountSummaryListItem,
            cardStyle = CardStyle.Full,
            clickable = false,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(Modifier.height(16.dp))

        if (viewState.showResendCodeButton) {
            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.verification_code),
                value = viewState.input,
                onValueChange = handler.onInputChanged,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (viewState.isContinueButtonEnabled) {
                            handler.onContinueClick()
                        } else {
                            defaultKeyboardAction(ImeAction.Done)
                        }
                    },
                ),
                autoFocus = true,
                cardStyle = CardStyle.Full,
                passwordFieldTestTag = "VerificationCodeEntry",
                showPasswordTestTag = "VerificationCodeVisibilityToggle",
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        } else {
            BitwardenPasswordField(
                label = stringResource(BitwardenString.master_password),
                value = viewState.input,
                onValueChange = handler.onInputChanged,
                showPasswordTestTag = "PasswordVisibilityToggle",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (viewState.isContinueButtonEnabled) {
                            handler.onContinueClick()
                        } else {
                            defaultKeyboardAction(ImeAction.Done)
                        }
                    },
                ),
                autoFocus = true,
                supportingText = stringResource(BitwardenString.vault_locked_master_password),
                passwordFieldTestTag = "MasterPasswordEntry",
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        Spacer(Modifier.height(16.dp))

        BitwardenFilledButton(
            label = stringResource(BitwardenString.continue_text),
            onClick = handler.onContinueClick,
            isEnabled = viewState.isContinueButtonEnabled,
            modifier = Modifier
                .testTag("ContinueImportButton")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        if (viewState.showResendCodeButton) {
            BitwardenOutlinedButton(
                label = stringResource(BitwardenString.resend_code),
                onClick = handler.onSendCodeClick,
                modifier = Modifier
                    .testTag("ResendTOTPCodeButton")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun VerifyPasswordContent_MasterPassword_preview() {
    ExportItemsScaffold(
        navIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
        onNavigationIconClick = {},
        navigationIconContentDescription = stringResource(BitwardenString.back),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        modifier = Modifier.fillMaxSize(),
    ) {
        VerifyPasswordContent(
            viewState = VerifyPasswordState.ViewState.Content(
                title = BitwardenString.verify_your_master_password.asText(),
                subtext = null,
                showResendCodeButton = false,
            ),
            accountSummaryListItem = AccountSelectionListItem(
                userId = "userId",
                isItemRestricted = false,
                avatarColorHex = "#FF0000",
                initials = "JD",
                email = "john.doe@example.com",
            ),
            handler = VerifyPasswordHandlers.createEmpty(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun VerifyPasswordContent_Otp_preview() {
    ExportItemsScaffold(
        navIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
        onNavigationIconClick = {},
        navigationIconContentDescription = stringResource(BitwardenString.back),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        modifier = Modifier.fillMaxSize(),
    ) {
        VerifyPasswordContent(
            viewState = VerifyPasswordState.ViewState.Content(
                title = BitwardenString.verify_your_account_email_address.asText(),
                subtext = BitwardenString
                    .enter_the_6_digit_code_that_was_emailed_to_the_address_below
                    .asText(),
                showResendCodeButton = true,
            ),
            accountSummaryListItem = AccountSelectionListItem(
                userId = "userId",
                isItemRestricted = false,
                avatarColorHex = "#FF0000",
                initials = "JD",
                email = "john.doe@example.com",
            ),
            handler = VerifyPasswordHandlers.createEmpty(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
