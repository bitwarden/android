package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.ReviewExportAction
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.ReviewExportViewModel

/**
 * A handler for the Review Export screen interactions, providing lambdas for UI events.
 *
 * @property onImportItemsClick Lambda to be invoked when the Import items button is clicked.
 * @property onCancelClick Lambda to be invoked when the "Cancel" button is clicked.
 * @property onDismissDialog Lambda to be invoked when a dialog is dismissed.
 */
data class ReviewExportHandlers(
    val onImportItemsClick: () -> Unit,
    val onSelectAnotherAccountClick: () -> Unit,
    val onCancelClick: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onNavigateBackClick: () -> Unit,
) {
    /**
     * Companion object for [ReviewExportHandlers].
     */
    companion object {
        /**
         * Creates a [ReviewExportHandlers] from a [ReviewExportViewModel].
         *
         * This function abstracts the creation of the handler, directly linking UI event lambdas
         * to the ViewModel's action dispatching mechanism.
         *
         * @param viewModel The [ReviewExportViewModel] instance to which actions will be sent.
         * @return A new instance of [ReviewExportHandlers].
         */
        fun create(viewModel: ReviewExportViewModel): ReviewExportHandlers = ReviewExportHandlers(
            onImportItemsClick = {
                viewModel.trySendAction(ReviewExportAction.ImportItemsClick)
            },
            onSelectAnotherAccountClick = {
                viewModel.trySendAction(ReviewExportAction.SelectAnotherAccountClick)
            },
            onCancelClick = {
                viewModel.trySendAction(ReviewExportAction.CancelClick)
            },
            onDismissDialog = {
                viewModel.trySendAction(ReviewExportAction.DismissDialog)
            },
            onNavigateBackClick = {
                viewModel.trySendAction(ReviewExportAction.NavigateBackClick)
            },
        )
    }
}

/**
 * Remembers a [ReviewExportHandlers] instance for the Review Export screen.
 *
 * This composable function utilizes the [ReviewExportHandlers.create] method to construct
 * the handler and remembers it across recompositions. This ensures that the same handler instance
 * (with stable lambdas tied to the provided ViewModel) is used, optimizing performance and
 * adhering to Compose best practices.
 *
 * @param viewModel The [ReviewExportViewModel] that will process the actions.
 * @return A remembered instance of [ReviewExportHandlers].
 */
@Composable
@OmitFromCoverage
fun rememberReviewExportHandler(
    viewModel: ReviewExportViewModel,
): ReviewExportHandlers = remember(viewModel) { ReviewExportHandlers.create(viewModel) }
