package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsAction
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsScreen
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsViewModel

/**
 * Action handlers for the [MigrateToMyItemsScreen].
 */
class MigrateToMyItemsHandler(
    val onContinueClick: () -> Unit,
    val onDeclineClick: () -> Unit,
    val onHelpClick: () -> Unit,
    val onDismissDialog: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates an instance of [MigrateToMyItemsHandler] using the provided
         * [MigrateToMyItemsViewModel].
         */
        fun create(viewModel: MigrateToMyItemsViewModel) = MigrateToMyItemsHandler(
            onContinueClick = {
                viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)
            },
            onDeclineClick = {
                viewModel.trySendAction(MigrateToMyItemsAction.DeclineAndLeaveClicked)
            },
            onHelpClick = {
                viewModel.trySendAction(MigrateToMyItemsAction.HelpLinkClicked)
            },
            onDismissDialog = {
                viewModel.trySendAction(MigrateToMyItemsAction.DismissDialogClicked)
            },
        )
    }
}

/**
 * Helper function to remember a [MigrateToMyItemsHandler] instance in a [Composable] scope.
 */
@Composable
fun rememberMigrateToMyItemsHandler(viewModel: MigrateToMyItemsViewModel): MigrateToMyItemsHandler =
    remember(viewModel) { MigrateToMyItemsHandler.create(viewModel) }
