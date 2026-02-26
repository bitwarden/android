package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountAction
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountViewModel

/**
 * Responsible for handling user interactions for the select account screen.
 */
data class SelectAccountHandlers(
    val onCloseClick: () -> Unit,
    val onAccountClick: (userId: String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [SelectAccountHandlers] by binding actions to the provided
         * [SelectAccountViewModel].
         */
        fun create(viewModel: SelectAccountViewModel): SelectAccountHandlers =
            SelectAccountHandlers(
                onCloseClick = {
                    viewModel.trySendAction(SelectAccountAction.CloseClick)
                },
                onAccountClick = {
                    viewModel.trySendAction(SelectAccountAction.AccountClick(userId = it))
                },
            )
    }
}

/**
 * Helper function to remember a [SelectAccountHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberSelectAccountHandlers(viewModel: SelectAccountViewModel): SelectAccountHandlers =
    remember(viewModel) { SelectAccountHandlers.create(viewModel) }
