package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsAction
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsScreen
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsViewModel

/**
 * Action handlers for the [MigrateToMyItemsScreen].
 *
 * @property onAcceptClick Handler for when the user clicks the Accept button to accept migration.
 * @property onDeclineClick Handler for when the user clicks the decline and leave button.
 * @property onHelpClick Handler for when the user clicks the help link.
 * @property onDismissDialog Handler for when the user dismisses a dialog.
 */
class MigrateToMyItemsHandler(
    val onAcceptClick: () -> Unit,
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
            onAcceptClick = {
                viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)
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
