package com.x8bit.bitwarden.ui.auth.feature.startregistration.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationViewModel

/**
 * A collection of handler functions for managing actions within the context of the
 * [StartRegistrationScreen].
 */
data class StartRegistrationHandler(
    val onEmailInputChange: (String) -> Unit,
    val onNameInputChange: (String) -> Unit,
    val onEnvironmentTypeSelect: (Environment.Type) -> Unit,
    val onContinueClick: () -> Unit,
    val onTermsClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
    val onReceiveMarketingEmailsToggle: (Boolean) -> Unit,
    val onUnsubscribeMarketingEmailsClick: () -> Unit,
    val onServerGeologyHelpClick: () -> Unit,
    val onBackClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [StartRegistrationHandler] by binding actions to the provided
         * [StartRegistrationViewModel].
         */
        fun create(viewModel: StartRegistrationViewModel): StartRegistrationHandler {
            return StartRegistrationHandler(
                onEmailInputChange = {
                    viewModel.trySendAction(
                        StartRegistrationAction.EmailInputChange(
                            it,
                        ),
                    )
                },
                onNameInputChange = {
                    viewModel.trySendAction(
                        StartRegistrationAction.NameInputChange(
                            it,
                        ),
                    )
                },
                onEnvironmentTypeSelect = {
                    viewModel.trySendAction(
                        StartRegistrationAction.EnvironmentTypeSelect(
                            it,
                        ),
                    )
                },
                onContinueClick = {
                    viewModel.trySendAction(StartRegistrationAction.ContinueClick)
                },
                onTermsClick = { viewModel.trySendAction(StartRegistrationAction.TermsClick) },
                onPrivacyPolicyClick = {
                    viewModel.trySendAction(StartRegistrationAction.PrivacyPolicyClick)
                },
                onReceiveMarketingEmailsToggle = {
                    viewModel.trySendAction(
                        StartRegistrationAction.ReceiveMarketingEmailsToggle(it),
                    )
                },
                onUnsubscribeMarketingEmailsClick = {
                    viewModel.trySendAction(
                        StartRegistrationAction.UnsubscribeMarketingEmailsClick,
                    )
                },
                onServerGeologyHelpClick = {
                    viewModel.trySendAction(StartRegistrationAction.ServerGeologyHelpClick)
                },
                onBackClick = { viewModel.trySendAction(StartRegistrationAction.BackClick) },
            )
        }
    }
}

/**
 * Convenience function for creating a [StartRegistrationHandler] instance in a [Composable]
 * context.
 */
@Composable
fun rememberStartRegistrationHandler(viewModel: StartRegistrationViewModel) = remember(viewModel) {
    StartRegistrationHandler.create(viewModel)
}
