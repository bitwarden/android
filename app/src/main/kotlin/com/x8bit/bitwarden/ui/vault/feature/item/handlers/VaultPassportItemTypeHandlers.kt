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
 * @property onPassportNumberVisibilityClick Handles the user toggling the passport number
 *  reveal state. Retained for telemetry parity with other sensitive-field reveal patterns;
 *  the actual reveal is managed locally in the composable via [rememberSaveable].
 * @property onNationalIdentificationNumberVisibilityClick Handles the user toggling the
 *  national identification number reveal state. Retained for telemetry parity; the actual
 *  reveal is managed locally in the composable via [rememberSaveable].
 */
data class VaultPassportItemTypeHandlers(
    val onCopyPassportNumberClick: () -> Unit,
    val onCopyNationalIdentificationNumberClick: () -> Unit,
    val onPassportNumberVisibilityClick: (Boolean) -> Unit,
    val onNationalIdentificationNumberVisibilityClick: (Boolean) -> Unit,
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
                onPassportNumberVisibilityClick = { isVisible ->
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport.PassportNumberVisibilityClick(
                            isVisible = isVisible,
                        ),
                    )
                },
                onNationalIdentificationNumberVisibilityClick = { isVisible ->
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport
                            .NationalIdentificationNumberVisibilityClick(isVisible = isVisible),
                    )
                },
            )
    }
}
