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
    val onNavigateBack: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onImportFromAnotherAppClick: () -> Unit,
    val onImportFromComputerClick: () -> Unit,
    val onSyncFailedTryAgainClick: () -> Unit,
) {

    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [ImportItemsHandler] using the provided [ImportItemsViewModel].
         */
        fun create(viewModel: ImportItemsViewModel) = ImportItemsHandler(
            onNavigateBack = {
                viewModel.trySendAction(ImportItemsAction.BackClick)
            },
            onDismissDialog = {
                viewModel.trySendAction(ImportItemsAction.DismissDialog)
            },
            onImportFromAnotherAppClick = {
                viewModel.trySendAction(ImportItemsAction.ImportFromAnotherAppClick)
            },
            onImportFromComputerClick = {
                viewModel.trySendAction(ImportItemsAction.ImportFromComputerClick)
            },
            onSyncFailedTryAgainClick = {
                viewModel.trySendAction(ImportItemsAction.SyncFailedTryAgainClick)
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
