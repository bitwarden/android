package com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction

/**
 * A collection of handler functions specifically tailored for managing action within the context of
 * device user verification.
 *
 * @property onUserVerificationSuccess Handles the action when biometric verification is
 * successful.
 * @property onUserVerificationFail Handles the action when biometric verification fails.
 * @property onUserVerificationLockOut Handles the action when too many failed verification attempts
 * locks out the user for a period of time.
 */
data class VaultItemListingUserVerificationHandlers(
    val onUserVerificationSuccess: (selectedCipherView: CipherView) -> Unit,
    val onUserVerificationLockOut: () -> Unit,
    val onUserVerificationFail: () -> Unit,
    val onUserVerificationCancelled: () -> Unit,
    val onUserVerificationNotSupported: (selectedCipherId: String?) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultItemListingUserVerificationHandlers] by binding
         * actions to the provided [VaultItemListingViewModel].
         */
        fun create(
            viewModel: VaultItemListingViewModel,
        ): VaultItemListingUserVerificationHandlers =
            VaultItemListingUserVerificationHandlers(
                onUserVerificationSuccess = { selectedCipherView ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.UserVerificationSuccess(
                            selectedCipherView = selectedCipherView,
                        ),
                    )
                },
                onUserVerificationFail = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationFail)
                },
                onUserVerificationLockOut = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationLockOut)
                },
                onUserVerificationCancelled = {
                    viewModel.trySendAction(VaultItemListingsAction.UserVerificationCancelled)
                },
                onUserVerificationNotSupported = { selectedCipherId ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.UserVerificationNotSupported(
                            selectedCipherId = selectedCipherId,
                        ),
                    )
                },
            )
    }
}
