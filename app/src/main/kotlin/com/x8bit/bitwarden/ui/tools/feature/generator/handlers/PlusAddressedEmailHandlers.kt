package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction.MainType.Username.UsernameType.PlusAddressedEmail as PlusAddressedEmailAction

/**
 * A class dedicated to handling user interactions related to plus addressed email
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
data class PlusAddressedEmailHandlers(
    val onEmailChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [PlusAddressedEmailHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(viewModel: GeneratorViewModel): PlusAddressedEmailHandlers =
            PlusAddressedEmailHandlers(
                onEmailChange = { newEmail ->
                    viewModel.trySendAction(
                        PlusAddressedEmailAction.EmailTextChange(email = newEmail),
                    )
                },
            )
    }
}

/**
 * Helper function to remember a [PlusAddressedEmailHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberPlusAddressedEmailHandlers(viewModel: GeneratorViewModel): PlusAddressedEmailHandlers =
    remember(viewModel) {
        PlusAddressedEmailHandlers.create(viewModel = viewModel)
    }
