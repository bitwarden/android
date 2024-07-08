package com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction

/**
 * A collection of handler functions specifically tailored for managing action within the context of
 * biometric user verification.
 *
 * @property onBiometricsVerificationSuccess Handles the action when biometric verification is
 * successful.
 * @property onBiometricsVerificationFail Handles the action when biometric verification fails.
 * @property onBiometricsLockOut Handles the action when too many failed verification attempts locks
 * out the user for a period of time.
 */
data class VaultItemListingBiometricUserVerificationHandlers(
    val onBiometricsVerificationSuccess: (selectedCipherView: CipherView) -> Unit,
    val onBiometricsLockOut: () -> Unit,
    val onBiometricsVerificationFail: () -> Unit,
    val onBiometricsVerificationCancelled: () -> Unit,
) {
    companion object {

        /**
         * Creates an instance of [VaultItemListingBiometricUserVerificationHandlers] by binding
         * actions to the provided [VaultItemListingViewModel].
         */
        fun create(
            viewModel: VaultItemListingViewModel,
        ): VaultItemListingBiometricUserVerificationHandlers =
            VaultItemListingBiometricUserVerificationHandlers(
                onBiometricsVerificationSuccess = { selectedCipherView ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.UserVerificationSuccess(
                            selectedCipherView = selectedCipherView,
                        ),
                    )
                },
                onBiometricsVerificationFail = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationLockOut)
                },
                onBiometricsLockOut = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationFail)
                },
                onBiometricsVerificationCancelled = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)
                },
            )
    }
}
