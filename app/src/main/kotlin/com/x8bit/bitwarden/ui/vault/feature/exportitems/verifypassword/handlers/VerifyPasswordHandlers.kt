package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.VerifyPasswordAction
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.VerifyPasswordViewModel

/**
 * A handler for the VerifyPassword screen interactions.
 */
data class VerifyPasswordHandlers(
    val onNavigateBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
    val onInputChanged: (String) -> Unit,
    val onSendCodeClick: () -> Unit,
    val onDismissDialog: () -> Unit,
) {

    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates a [VerifyPasswordHandlers] from a [VerifyPasswordViewModel].
         */
        fun create(viewModel: VerifyPasswordViewModel): VerifyPasswordHandlers =
            VerifyPasswordHandlers(
                onNavigateBackClick = {
                    viewModel.trySendAction(VerifyPasswordAction.NavigateBackClick)
                },
                onContinueClick = {
                    viewModel.trySendAction(VerifyPasswordAction.ContinueClick)
                },
                onInputChanged = {
                    viewModel.trySendAction(
                        VerifyPasswordAction.PasswordInputChangeReceive(it),
                    )
                },
                onSendCodeClick = {
                    viewModel.trySendAction(VerifyPasswordAction.ResendCodeClick)
                },
                onDismissDialog = {
                    viewModel.trySendAction(VerifyPasswordAction.DismissDialog)
                },
            )
    }
}

/**
 * Helper function to remember a [VerifyPasswordHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberVerifyPasswordHandler(viewModel: VerifyPasswordViewModel): VerifyPasswordHandlers =
    remember(viewModel) { VerifyPasswordHandlers.create(viewModel) }
