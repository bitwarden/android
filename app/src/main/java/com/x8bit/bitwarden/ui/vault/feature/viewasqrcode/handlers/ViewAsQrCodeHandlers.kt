package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.handlers

import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.ViewAsQrCodeAction
import com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.ViewAsQrCodeViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing as QR code.
 */
data class ViewAsQrCodeHandlers(
    val onBackClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates the [ViewAsQrCodeHandlers] using the [ViewAsQrCodeViewModel] to send desired
         * actions.
         */
        fun create(viewModel: ViewAsQrCodeViewModel): ViewAsQrCodeHandlers =
            ViewAsQrCodeHandlers(
                onBackClick = { viewModel.trySendAction(ViewAsQrCodeAction.BackClick) },
            )
    }
}
