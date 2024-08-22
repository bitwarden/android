package com.x8bit.bitwarden.ui.autofill.fido2.handlers

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.autofill.fido2.Fido2Action
import com.x8bit.bitwarden.ui.autofill.fido2.Fido2ViewModel

/**
 * Device based user verification handlers for [Fido2ViewModel].
 */
data class Fido2UserVerificationHandlers(
    val onUserVerificationSuccess: (selectedCipherView: CipherView) -> Unit,
    val onUserVerificationLockOut: () -> Unit,
    val onUserVerificationFail: () -> Unit,
    val onUserVerificationCancelled: () -> Unit,
    val onUserVerificationNotSupported: (selectedCipherId: String?) -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [Fido2UserVerificationHandlers] by binding
         * actions to the provided [Fido2ViewModel].
         */
        fun create(
            viewModel: Fido2ViewModel,
        ): Fido2UserVerificationHandlers =
            Fido2UserVerificationHandlers(
                onUserVerificationSuccess = { selectedCipherView ->
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationSuccess(
                            selectedCipherView = selectedCipherView,
                        ),
                    )
                },
                onUserVerificationFail = {
                    viewModel.trySendAction(Fido2Action.DeviceUserVerificationFail)
                },
                onUserVerificationLockOut = {
                    viewModel.trySendAction(Fido2Action.DeviceUserVerificationLockOut)
                },
                onUserVerificationCancelled = {
                    viewModel.trySendAction(Fido2Action.DeviceUserVerificationCancelled)
                },
                onUserVerificationNotSupported = { selectedCipherId ->
                    viewModel.trySendAction(
                        Fido2Action.DeviceUserVerificationNotSupported(
                            selectedCipherId = selectedCipherId,
                        ),
                    )
                },
            )
    }
}
