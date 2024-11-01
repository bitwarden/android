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
 * @property onPrivateKeyVisibilityChange Handler for toggling the visibility of the private key.
 */
data class VaultAddEditSshKeyTypeHandlers(
    val onPrivateKeyVisibilityChange: (Boolean) -> Unit,
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

                onPrivateKeyVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange(
                            isVisible = it,
                        ),
                    )
                },
            )
    }
}
