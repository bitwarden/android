package com.x8bit.bitwarden.ui.auth.feature.checkemail.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.auth.feature.checkemail.CheckEmailAction
import com.x8bit.bitwarden.ui.auth.feature.checkemail.CheckEmailViewModel

/**
 * Handler for [CheckEmailScreen] actions.
 */
class CheckEmailHandler(
    val onOpenEmailAppClick: () -> Unit,
    val onChangeEmailClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onLoginClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Create [CheckEmailHandler] with the given [viewModel] to send actions to.
         */
        fun create(viewModel: CheckEmailViewModel) = CheckEmailHandler(
            onChangeEmailClick = { viewModel.trySendAction(CheckEmailAction.ChangeEmailClick) },
            onOpenEmailAppClick = { viewModel.trySendAction(CheckEmailAction.OpenEmailClick) },
            onLoginClick = { viewModel.trySendAction(CheckEmailAction.LoginClick) },
            onBackClick = { viewModel.trySendAction(CheckEmailAction.BackClick) },
        )
    }
}

/**
 * Remember [CheckEmailHandler] with the given [viewModel] within a [Composable] scope.
 */
@Composable
fun rememberCheckEmailHandler(viewModel: CheckEmailViewModel) =
    remember(viewModel) {
        CheckEmailHandler.create(viewModel)
    }
