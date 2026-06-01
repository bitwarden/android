package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction.MainType.Username.UsernameType.RandomWord as RandomWordAction

/**
 * A class dedicated to handling user interactions related to Random Word
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
data class RandomWordHandlers(
    val onCapitalizeChange: (Boolean) -> Unit,
    val onIncludeNumberChange: (Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [RandomWordHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): RandomWordHandlers = RandomWordHandlers(
            onCapitalizeChange = { shouldCapitalize ->
                viewModel.trySendAction(
                    RandomWordAction.ToggleCapitalizeChange(capitalize = shouldCapitalize),
                )
            },
            onIncludeNumberChange = { shouldIncludeNumber ->
                viewModel.trySendAction(
                    RandomWordAction.ToggleIncludeNumberChange(includeNumber = shouldIncludeNumber),
                )
            },
        )
    }
}

/**
 * Helper function to remember a [RandomWordHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberRandomWordHandlers(viewModel: GeneratorViewModel): RandomWordHandlers =
    remember(viewModel) {
        RandomWordHandlers.create(viewModel = viewModel)
    }
