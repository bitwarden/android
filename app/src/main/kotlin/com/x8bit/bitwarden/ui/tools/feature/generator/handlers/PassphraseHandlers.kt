package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel

/**
 * A class dedicated to handling user interactions related to passphrase configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
data class PassphraseHandlers(
    val onPassphraseNumWordsCounterChange: (Int) -> Unit,
    val onPassphraseWordSeparatorChange: (Char?) -> Unit,
    val onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    val onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [PassphraseHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): PassphraseHandlers = PassphraseHandlers(
            onPassphraseNumWordsCounterChange = { changeInCounter ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.NumWordsCounterChange(
                        numWords = changeInCounter,
                    ),
                )
            },
            onPassphraseWordSeparatorChange = { newSeparator ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.WordSeparatorTextChange(
                        wordSeparator = newSeparator,
                    ),
                )
            },
            onPassphraseCapitalizeToggleChange = { shouldCapitalize ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange(
                        capitalize = shouldCapitalize,
                    ),
                )
            },
            onPassphraseIncludeNumberToggleChange = { shouldIncludeNumber ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange(
                        includeNumber = shouldIncludeNumber,
                    ),
                )
            },
        )
    }
}

/**
 * Helper function to remember a [PassphraseHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberPassphraseHandlers(viewModel: GeneratorViewModel): PassphraseHandlers =
    remember(viewModel) {
        PassphraseHandlers.create(viewModel = viewModel)
    }
