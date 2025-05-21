package com.x8bit.bitwarden.ui.auth.feature.accountsetup.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupAutoFillAction
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupAutoFillViewModel

/**
 * Handler for the Auto-fill setup screen.
 */
data class SetupAutoFillHandler(
    val onAutofillServiceChanged: (Boolean) -> Unit,
    val onContinueClick: () -> Unit,
    val onTurnOnLaterClick: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onConfirmTurnOnLaterClick: () -> Unit,
    val sendAutoFillServiceFallback: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Convenience function for creating a [SetupAutoFillHandler] with a
         * [SetupAutoFillViewModel].
         */
        fun create(viewModel: SetupAutoFillViewModel): SetupAutoFillHandler = SetupAutoFillHandler(
            onAutofillServiceChanged = {
                viewModel.trySendAction(
                    SetupAutoFillAction.AutofillServiceChanged(
                        it,
                    ),
                )
            },
            onContinueClick = { viewModel.trySendAction(SetupAutoFillAction.ContinueClick) },
            onTurnOnLaterClick = { viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick) },
            onDismissDialog = { viewModel.trySendAction(SetupAutoFillAction.DismissDialog) },
            onConfirmTurnOnLaterClick = {
                viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterConfirmClick)
            },
            sendAutoFillServiceFallback = {
                viewModel.trySendAction(SetupAutoFillAction.AutoFillServiceFallback)
            },
        )
    }
}

/**
 * Convenience function for creating a [SetupAutoFillHandler] in a [Composable] scope.
 */
@Composable
fun rememberSetupAutoFillHandler(viewModel: SetupAutoFillViewModel): SetupAutoFillHandler =
    remember(viewModel) {
        SetupAutoFillHandler.create(viewModel)
    }
