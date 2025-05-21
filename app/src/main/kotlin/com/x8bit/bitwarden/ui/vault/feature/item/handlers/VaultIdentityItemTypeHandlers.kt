package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing identity
 * items in a vault.
 */
data class VaultIdentityItemTypeHandlers(
    val onCopyIdentityNameClick: () -> Unit,
    val onCopyUsernameClick: () -> Unit,
    val onCopyCompanyClick: () -> Unit,
    val onCopySsnClick: () -> Unit,
    val onCopyPassportNumberClick: () -> Unit,
    val onCopyLicenseNumberClick: () -> Unit,
    val onCopyEmailClick: () -> Unit,
    val onCopyPhoneClick: () -> Unit,
    val onCopyAddressClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass", "MaxLineLength")
    companion object {
        /**
         * Creates the [VaultIdentityItemTypeHandlers] using the [viewModel] to send desired actions.
         */
        fun create(
            viewModel: VaultItemViewModel,
        ): VaultIdentityItemTypeHandlers =
            VaultIdentityItemTypeHandlers(
                onCopyIdentityNameClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyIdentityNameClick)
                },
                onCopyUsernameClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyUsernameClick)
                },
                onCopyCompanyClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyCompanyClick)
                },
                onCopySsnClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopySsnClick)
                },
                onCopyPassportNumberClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPassportNumberClick)
                },
                onCopyLicenseNumberClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyLicenseNumberClick)
                },
                onCopyEmailClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyEmailClick)
                },
                onCopyPhoneClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPhoneClick)
                },
                onCopyAddressClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyAddressClick)
                },
            )
    }
}
