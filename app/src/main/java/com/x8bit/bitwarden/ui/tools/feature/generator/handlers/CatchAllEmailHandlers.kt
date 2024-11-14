package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction.MainType.Username.UsernameType.CatchAllEmail as CatchAllEmailAction

/**
 * A class dedicated to handling user interactions related to plus addressed email
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
data class CatchAllEmailHandlers(
    val onDomainChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [CatchAllEmailHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): CatchAllEmailHandlers = CatchAllEmailHandlers(
            onDomainChange = { newDomain ->
                viewModel.trySendAction(CatchAllEmailAction.DomainTextChange(domain = newDomain))
            },
        )
    }
}

/**
 * Helper function to remember a [CatchAllEmailHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberCatchAllEmailHandlers(viewModel: GeneratorViewModel): CatchAllEmailHandlers =
    remember(viewModel) {
        CatchAllEmailHandlers.create(viewModel = viewModel)
    }
