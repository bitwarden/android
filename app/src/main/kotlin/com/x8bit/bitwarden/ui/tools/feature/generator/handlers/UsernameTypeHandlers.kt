package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel

/**
 * A class dedicated to handling user interactions related to all username configurations.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
data class UsernameTypeHandlers(
    val onUsernameTooltipClicked: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [UsernameTypeHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): UsernameTypeHandlers = UsernameTypeHandlers(
            onUsernameTooltipClicked = {
                viewModel.trySendAction(GeneratorAction.MainType.Username.UsernameType.TooltipClick)
            },
        )
    }
}

/**
 * Helper function to remember a [UsernameTypeHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberUsernameTypeHandlers(viewModel: GeneratorViewModel): UsernameTypeHandlers =
    remember(viewModel) {
        UsernameTypeHandlers.create(viewModel = viewModel)
    }
