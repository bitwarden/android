package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel

/**
 * A class dedicated to handling user interactions related to password configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
data class PasswordHandlers(
    val onPasswordSliderLengthChange: (Int, Boolean) -> Unit,
    val onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    val onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    val onPasswordToggleNumbersChange: (Boolean) -> Unit,
    val onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    val onPasswordMinNumbersCounterChange: (Int) -> Unit,
    val onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    val onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [PasswordHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): PasswordHandlers = PasswordHandlers(
            onPasswordSliderLengthChange = { newLength, isUserInteracting ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.SliderLengthChange(
                        length = newLength,
                        isUserInteracting = isUserInteracting,
                    ),
                )
            },
            onPasswordToggleCapitalLettersChange = { shouldUseCapitals ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                        useCapitals = shouldUseCapitals,
                    ),
                )
            },
            onPasswordToggleLowercaseLettersChange = { shouldUseLowercase ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                        useLowercase = shouldUseLowercase,
                    ),
                )
            },
            onPasswordToggleNumbersChange = { shouldUseNumbers ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleNumbersChange(
                        useNumbers = shouldUseNumbers,
                    ),
                )
            },
            onPasswordToggleSpecialCharactersChange = { shouldUseSpecialChars ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                        useSpecialChars = shouldUseSpecialChars,
                    ),
                )
            },
            onPasswordMinNumbersCounterChange = { newMinNumbers ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinNumbersCounterChange(
                        minNumbers = newMinNumbers,
                    ),
                )
            },
            onPasswordMinSpecialCharactersChange = { newMinSpecial ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.MinSpecialCharactersChange(
                        minSpecial = newMinSpecial,
                    ),
                )
            },
            onPasswordToggleAvoidAmbiguousCharsChange = { shouldAvoidAmbiguousChars ->
                viewModel.trySendAction(
                    GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange(
                        avoidAmbiguousChars = shouldAvoidAmbiguousChars,
                    ),
                )
            },
        )
    }
}

/**
 * Helper function to remember a [PasswordHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberPasswordHandlers(viewModel: GeneratorViewModel): PasswordHandlers =
    remember(viewModel) {
        PasswordHandlers.create(viewModel = viewModel)
    }
