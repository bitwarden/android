package com.x8bit.bitwarden.ui.vault.feature.item.handlers

import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing
 * passport items in a vault.
 *
 * Currently no copy or hidden field actions are needed for passport per spec.
 */
data class VaultPassportItemTypeHandlers(
    val placeholder: Unit = Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates the [VaultPassportItemTypeHandlers] using the [viewModel] to send
         * desired actions.
         */
        fun create(
            @Suppress("UNUSED_PARAMETER") viewModel: VaultItemViewModel,
        ): VaultPassportItemTypeHandlers =
            VaultPassportItemTypeHandlers()
    }
}
