package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * passport items in a vault.
 *
 * @property onCopyGivenNameClick Handles the user clicking the copy button next to the
 *  given name.
 * @property onCopySurnameClick Handles the user clicking the copy button next to the
 *  surname.
 * @property onCopyPassportNumberClick Handles the user clicking the copy button next to the
 *  passport number.
 * @property onCopyNationalIdentificationNumberClick Handles the user clicking the copy button
 *  next to the national identification number.
 */
data class VaultPassportItemTypeHandlers(
    val onCopyGivenNameClick: () -> Unit,
    val onCopySurnameClick: () -> Unit,
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
                onCopyGivenNameClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport.CopyGivenNameClick,
                    )
                },
                onCopySurnameClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Passport.CopySurnameClick,
                    )
                },
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

/**
 * Helper function to remember a [VaultPassportItemTypeHandlers] instance in a [Composable]
 * scope.
 */
@Composable
fun rememberVaultPassportItemTypeHandlers(
    viewModel: VaultItemViewModel,
): VaultPassportItemTypeHandlers =
    remember(viewModel) {
        VaultPassportItemTypeHandlers.create(viewModel = viewModel)
    }
