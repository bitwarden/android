package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * driver's license items in a vault.
 *
 * @property onCopyFirstNameClick Handles the user clicking the copy button next to the first
 *  name.
 * @property onCopyMiddleNameClick Handles the user clicking the copy button next to the middle
 *  name.
 * @property onCopyLastNameClick Handles the user clicking the copy button next to the last name.
 * @property onCopyLicenseNumberClick Handles the user clicking the copy button next to the
 *  license number.
 */
data class VaultDriversLicenseItemTypeHandlers(
    val onCopyFirstNameClick: () -> Unit,
    val onCopyMiddleNameClick: () -> Unit,
    val onCopyLastNameClick: () -> Unit,
    val onCopyLicenseNumberClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates the [VaultDriversLicenseItemTypeHandlers] using the [viewModel] to send
         * desired actions.
         */
        fun create(viewModel: VaultItemViewModel): VaultDriversLicenseItemTypeHandlers =
            VaultDriversLicenseItemTypeHandlers(
                onCopyFirstNameClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.DriversLicense.CopyFirstNameClick,
                    )
                },
                onCopyMiddleNameClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.DriversLicense.CopyMiddleNameClick,
                    )
                },
                onCopyLastNameClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.DriversLicense.CopyLastNameClick,
                    )
                },
                onCopyLicenseNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.DriversLicense.CopyLicenseNumberClick,
                    )
                },
            )
    }
}
