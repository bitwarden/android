package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * driver's license items in a vault.
 *
 * @property onCopyLicenseNumberClick Handles the user clicking the copy button next to the
 *  license number.
 */
data class VaultDriversLicenseItemTypeHandlers(
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
                onCopyLicenseNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.DriversLicense.CopyLicenseNumberClick,
                    )
                },
            )
    }
}
