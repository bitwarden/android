package com.x8bit.bitwarden.ui.auth.feature.completeregistration.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationViewModel

/**
 * Handler for the complete registration screen lambda invocations.
 */
@Suppress("LongParameterList")
class CompleteRegistrationHandler(
    val onDismissErrorDialog: () -> Unit,
    val onContinueWithBreachedPasswordClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onPasswordInputChange: (String) -> Unit,
    val onConfirmPasswordInputChange: (String) -> Unit,
    val onPasswordHintChange: (String) -> Unit,
    val onCheckDataBreachesToggle: (Boolean) -> Unit,
    val onMakeStrongPassword: () -> Unit,
    val onLearnToPreventLockout: () -> Unit,
    val onCallToAction: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Create [CompleteRegistrationHandler] with the given [viewModel] to send actions to.
         */
        fun create(viewModel: CompleteRegistrationViewModel) = CompleteRegistrationHandler(
            onDismissErrorDialog = {
                viewModel.trySendAction(CompleteRegistrationAction.ErrorDialogDismiss)
            },
            onContinueWithBreachedPasswordClick = {
                viewModel.trySendAction(
                    CompleteRegistrationAction.ContinueWithBreachedPasswordClick,
                )
            },
            onBackClick = { viewModel.trySendAction(CompleteRegistrationAction.BackClick) },
            onPasswordInputChange = {
                viewModel.trySendAction(
                    CompleteRegistrationAction.PasswordInputChange(
                        it,
                    ),
                )
            },
            onConfirmPasswordInputChange = {
                viewModel.trySendAction(
                    CompleteRegistrationAction.ConfirmPasswordInputChange(
                        it,
                    ),
                )
            },
            onPasswordHintChange = {
                viewModel.trySendAction(
                    CompleteRegistrationAction.PasswordHintChange(
                        it,
                    ),
                )
            },
            onCheckDataBreachesToggle = {
                viewModel.trySendAction(
                    CompleteRegistrationAction.CheckDataBreachesToggle(
                        it,
                    ),
                )
            },
            onMakeStrongPassword = {
                viewModel.trySendAction(CompleteRegistrationAction.MakePasswordStrongClick)
            },
            onLearnToPreventLockout = {
                viewModel.trySendAction(CompleteRegistrationAction.LearnToPreventLockoutClick)
            },
            onCallToAction = {
                viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
            },
        )
    }
}

/**
 * Remember [CompleteRegistrationHandler] with the given [viewModel] within a [Composable] scope.
 */
@Composable
fun rememberCompleteRegistrationHandler(viewModel: CompleteRegistrationViewModel) =
    remember(viewModel) {
        CompleteRegistrationHandler.create(viewModel)
    }
