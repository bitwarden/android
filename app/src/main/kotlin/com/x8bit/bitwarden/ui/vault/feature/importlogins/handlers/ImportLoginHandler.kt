package com.x8bit.bitwarden.ui.vault.feature.importlogins.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.importlogins.ImportLoginsAction
import com.x8bit.bitwarden.ui.vault.feature.importlogins.ImportLoginsViewModel

/**
 * Action handlers for the [com.x8bit.bitwarden.ui.vault.feature.importlogins.ImportLoginsScreen].
 */
data class ImportLoginHandler(
    val onGetStartedClick: () -> Unit,
    val onImportLaterClick: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onConfirmGetStarted: () -> Unit,
    val onConfirmImportLater: () -> Unit,
    val onCloseClick: () -> Unit,
    val onHelpClick: () -> Unit,
    val onMoveToInitialContent: () -> Unit,
    val onMoveToStepOne: () -> Unit,
    val onMoveToStepTwo: () -> Unit,
    val onMoveToStepThree: () -> Unit,
    val onMoveToSyncInProgress: () -> Unit,
    val onRetrySync: () -> Unit,
    val onFailedSyncAcknowledged: () -> Unit,
    val onSuccessfulSyncAcknowledged: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [ImportLoginHandler] using the provided [ImportLoginsViewModel].
         */
        fun create(viewModel: ImportLoginsViewModel) = ImportLoginHandler(
            onGetStartedClick = { viewModel.trySendAction(ImportLoginsAction.GetStartedClick) },
            onImportLaterClick = { viewModel.trySendAction(ImportLoginsAction.ImportLaterClick) },
            onDismissDialog = { viewModel.trySendAction(ImportLoginsAction.DismissDialog) },
            onConfirmGetStarted = { viewModel.trySendAction(ImportLoginsAction.ConfirmGetStarted) },
            onConfirmImportLater = {
                viewModel.trySendAction(ImportLoginsAction.ConfirmImportLater)
            },
            onCloseClick = { viewModel.trySendAction(ImportLoginsAction.CloseClick) },
            onHelpClick = { viewModel.trySendAction(ImportLoginsAction.HelpClick) },
            onMoveToInitialContent = {
                viewModel.trySendAction(ImportLoginsAction.MoveToInitialContent)
            },
            onMoveToStepOne = { viewModel.trySendAction(ImportLoginsAction.MoveToStepOne) },
            onMoveToStepTwo = { viewModel.trySendAction(ImportLoginsAction.MoveToStepTwo) },
            onMoveToStepThree = { viewModel.trySendAction(ImportLoginsAction.MoveToStepThree) },
            onMoveToSyncInProgress = {
                viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
            },
            onRetrySync = { viewModel.trySendAction(ImportLoginsAction.RetryVaultSync) },
            onFailedSyncAcknowledged = {
                viewModel.trySendAction(ImportLoginsAction.FailedSyncAcknowledged)
            },
            onSuccessfulSyncAcknowledged = {
                viewModel.trySendAction(ImportLoginsAction.SuccessfulSyncAcknowledged)
            },
        )
    }
}

/**
 * Helper function to remember a [ImportLoginHandler] instance in a [Composable] scope.
 */
@Composable
fun rememberImportLoginHandler(viewModel: ImportLoginsViewModel): ImportLoginHandler =
    remember(viewModel) {
        ImportLoginHandler.create(viewModel = viewModel)
    }
