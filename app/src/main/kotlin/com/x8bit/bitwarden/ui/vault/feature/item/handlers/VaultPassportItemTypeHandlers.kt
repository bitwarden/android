package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * passport items in a vault.
 *
 * @property onCopyPassportNumberClick Handles the user clicking the copy button next to the
 *  passport number.
 * @property onCopyNationalIdentificationNumberClick Handles the user clicking the copy button
 *  next to the national identification number.
 */
data class VaultPassportItemTypeHandlers(
    val onCopyPassportNumberClick: () -> Unit,
    val onCopyNationalIdentificationNumberClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates the [VaultPassportItemTypeHandlers] using the [viewModel] to send
         * desired actions.
         */
        fun create(viewModel: VaultItemViewModel): VaultPassportItemTypeHandlers =
            VaultPassportItemTypeHandlers(
                onCopyPassportNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport.CopyPassportNumberClick,
                    )
                },
                onCopyNationalIdentificationNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport
                            .CopyNationalIdentificationNumberClick,
                    )
                },
            )
    }
}
