@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.importitems.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.importitems.ImportItemsAction
import com.x8bit.bitwarden.ui.vault.feature.importitems.ImportItemsScreen
import com.x8bit.bitwarden.ui.vault.feature.importitems.ImportItemsViewModel

/**
 * Action handlers for the [ImportItemsScreen].
 */
@OmitFromCoverage
data class ImportItemsHandler(
    val onReturnToVaultClick: () -> Unit,
    val onNavigateBack: () -> Unit,
    val onGetStartedClick: () -> Unit,
) {

    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [ImportItemsHandler] using the provided [ImportItemsViewModel].
         */
        fun create(viewModel: ImportItemsViewModel) = ImportItemsHandler(
            onReturnToVaultClick = {
                viewModel.trySendAction(ImportItemsAction.ReturnToVaultClick)
            },
            onNavigateBack = {
                viewModel.trySendAction(ImportItemsAction.BackClick)
            },
            onGetStartedClick = {
                viewModel.trySendAction(ImportItemsAction.GetStartedClick)
            },
        )
    }
}

/**
 * Helper function to remember a [ImportItemsHandler] instance in a [Composable] scope.
 */
@Composable
fun rememberImportItemsHandler(viewModel: ImportItemsViewModel): ImportItemsHandler =
    remember(viewModel) { ImportItemsHandler.create(viewModel) }
