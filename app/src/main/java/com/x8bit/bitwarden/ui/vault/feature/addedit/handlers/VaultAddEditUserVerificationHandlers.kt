package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel

/**
 * A collection of handler functions specifically tailored for managing action within the context of
 * biometric user verification.
 *
 * @property onUserVerificationSuccess Handles the action when biometric verification is
 * successful.
 * @property onUserVerificationFail Handles the action when biometric verification fails.
 * @property onUserVerificationLockOut Handles the action when too many failed verification attempts
 * locks out the user for a period of time.
 * @property onUserVerificationCancelled Handles the action when verification is explicitly
 * cancelled by the user.
 * @property onUserVerificationNotSupported Handles the action when device biometric and credential
 * verification cannot be performed.
 */
data class VaultAddEditUserVerificationHandlers(
    val onUserVerificationSuccess: () -> Unit,
    val onUserVerificationLockOut: () -> Unit,
    val onUserVerificationFail: () -> Unit,
    val onUserVerificationCancelled: () -> Unit,
    val onUserVerificationNotSupported: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [VaultAddEditUserVerificationHandlers] by binding actions
         * to the provided [VaultAddEditViewModel].
         */
        fun create(
            viewModel: VaultAddEditViewModel,
        ): VaultAddEditUserVerificationHandlers =
            VaultAddEditUserVerificationHandlers(
                onUserVerificationSuccess = {
                    viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess)
                },
                onUserVerificationFail = {
                    viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationFail)
                },
                onUserVerificationLockOut = {
                    viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationLockOut)
                },
                onUserVerificationCancelled = {
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.UserVerificationCancelled,
                    )
                },
                onUserVerificationNotSupported = {
                    viewModel.trySendAction(
                        VaultAddEditAction.Common.UserVerificationNotSupported,
                    )
                },
            )
    }
}
