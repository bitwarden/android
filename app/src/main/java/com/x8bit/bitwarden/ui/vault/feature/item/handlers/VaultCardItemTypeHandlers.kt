package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemAction
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing card
 * items in a vault.
 */
data class VaultCardItemTypeHandlers(
    val onCopyNumberClick: () -> Unit,
    val onCopySecurityCodeClick: () -> Unit,
    val onShowNumberClick: (Boolean) -> Unit,
    val onShowSecurityCodeClick: (Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates the [VaultCardItemTypeHandlers] using the [viewModel] to send desired actions.
         */
        fun create(viewModel: VaultItemViewModel): VaultCardItemTypeHandlers =
            VaultCardItemTypeHandlers(
                onCopyNumberClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)
                },
                onCopySecurityCodeClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)
                },
                onShowNumberClick = {
                    viewModel.trySendAction(
                        VaultItemAction.ItemType.Card.NumberVisibilityClick(it),
                    )
                },
                onShowSecurityCodeClick = {
                    viewModel.trySendAction(VaultItemAction.ItemType.Card.CodeVisibilityClick(it))
                },
            )
    }
}
