package com.x8bit.bitwarden.ui.vault.feature.addedit.handlers

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditViewModel

/**
 * Provides a set of handlers for interactions related to SSH key types within the vault add/edit
 * screen.
 *
 * These handlers are used to update the ViewModel with user actions such as text changes and
 * visibility changes for different SSH key fields (public key, private key, fingerprint).
 *
 * @property onPublicKeyTextChange Handler for changes in the public key text field.
 * @property onPrivateKeyTextChange Handler for changes in the private key text field.
 * @property onPrivateKeyVisibilityChange Handler for toggling the visibility of the private key.
 * @property onFingerprintTextChange Handler for changes in the fingerprint text field.
 */
data class VaultAddEditSshKeyTypeHandlers(
    val onPublicKeyTextChange: (String) -> Unit,
    val onPrivateKeyTextChange: (String) -> Unit,
    val onPrivateKeyVisibilityChange: (Boolean) -> Unit,
    val onFingerprintTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultAddEditSshKeyTypeHandlers] with handlers that dispatch
         * actions to the provided ViewModel.
         *
         * @param viewModel The ViewModel to which actions will be dispatched.
         */
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditSshKeyTypeHandlers =
            VaultAddEditSshKeyTypeHandlers(
                onPublicKeyTextChange = { newPublicKey ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PublicKeyTextChange(
                            publicKey = newPublicKey,
                        ),
                    )
                },
                onPrivateKeyTextChange = { newPrivateKey ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PrivateKeyTextChange(
                            privateKey = newPrivateKey,
                        ),
                    )
                },
                onPrivateKeyVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange(
                            isVisible = it,
                        ),
                    )
                },
                onFingerprintTextChange = { newFingerprint ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.FingerprintTextChange(
                            fingerprint = newFingerprint,
                        ),
                    )
                },
            )
    }
}
