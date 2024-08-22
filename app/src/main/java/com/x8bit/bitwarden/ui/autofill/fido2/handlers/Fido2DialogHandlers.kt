package com.x8bit.bitwarden.ui.autofill.fido2.handlers

import com.x8bit.bitwarden.ui.autofill.fido2.Fido2Action
import com.x8bit.bitwarden.ui.autofill.fido2.Fido2ViewModel

/**
 * Dialog action handlers for [Fido2ViewModel].
 */
data class Fido2DialogHandlers(
    val onDismissDialogClick: () -> Unit,
    val onDismissUserVerification: () -> Unit,
    val onSubmitMasterPasswordFido2Verification: (
        password: String,
        selectedCipherId: String,
    ) -> Unit,
    val onSubmitPinFido2Verification: (pin: String, selectedCipherId: String) -> Unit,
    val onSubmitPinSetUpFido2Verification: (pin: String, selectedCipherId: String) -> Unit,
    val onRetryFido2PasswordVerification: (selectedCipherId: String) -> Unit,
    val onRetryFido2PinVerification: (selectedCipherId: String) -> Unit,
    val onRetryPinSetUpFido2Verification: (selectedCipherId: String) -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [Fido2DialogHandlers] by binding actions to the provided
         * [Fido2ViewModel].
         */
        fun create(viewModel: Fido2ViewModel): Fido2DialogHandlers =
            Fido2DialogHandlers(
                onDismissDialogClick = {
                    viewModel.trySendAction(Fido2Action.DismissDialogClick)
                },
                onDismissUserVerification = {
                    viewModel.trySendAction(Fido2Action.DismissBitwardenUserVerification)
                },
                onSubmitMasterPasswordFido2Verification = { password, selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.MasterPasswordFido2VerificationSubmit(
                            password = password,
                            selectedCipherId = selectedCipherId,
                        ),
                    )
                },
                onSubmitPinSetUpFido2Verification = { pin, selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.PinFido2SetUpSubmit(
                            pin = pin,
                            selectedCipherId = selectedCipherId,
                        ),
                    )
                },
                onSubmitPinFido2Verification = { pin, selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.PinFido2VerificationSubmit(
                            pin = pin,
                            selectedCipherId = selectedCipherId,
                        ),
                    )
                },
                onRetryFido2PasswordVerification = { selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PasswordVerificationClick(selectedCipherId),
                    )
                },
                onRetryFido2PinVerification = { selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PinVerificationClick(selectedCipherId),
                    )
                },
                onRetryPinSetUpFido2Verification = { selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.RetryFido2PinSetUpClick(selectedCipherId),
                    )
                },
            )
    }
}
