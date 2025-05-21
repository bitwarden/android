package com.x8bit.bitwarden.ui.vault.feature.verificationcode.handlers

import com.x8bit.bitwarden.ui.vault.feature.verificationcode.VerificationCodeAction
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.VerificationCodeViewModel

/**
 * A collection of handler functions for managing actions within the context of viewing a list of
 * verification code items.
 */
data class VerificationCodeHandlers(
    val backClick: () -> Unit,
    val searchIconClick: () -> Unit,
    val itemClick: (id: String) -> Unit,
    val refreshClick: () -> Unit,
    val syncClick: () -> Unit,
    val lockClick: () -> Unit,
    val copyClick: (text: String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VerificationCodeHandlers] by binding actions to the provided
         * [VerificationCodeViewModel].
         */
        fun create(
            viewModel: VerificationCodeViewModel,
        ): VerificationCodeHandlers =
            VerificationCodeHandlers(
                backClick = { viewModel.trySendAction(VerificationCodeAction.BackClick) },
                searchIconClick = {
                    viewModel.trySendAction(VerificationCodeAction.SearchIconClick)
                },
                itemClick = { viewModel.trySendAction(VerificationCodeAction.ItemClick(it)) },
                refreshClick = { viewModel.trySendAction(VerificationCodeAction.RefreshClick) },
                syncClick = { viewModel.trySendAction(VerificationCodeAction.SyncClick) },
                lockClick = { viewModel.trySendAction(VerificationCodeAction.LockClick) },
                copyClick = { viewModel.trySendAction(VerificationCodeAction.CopyClick(it)) },
            )
    }
}
