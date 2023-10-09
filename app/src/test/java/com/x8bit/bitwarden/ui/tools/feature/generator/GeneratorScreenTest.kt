@file:Suppress("MaxLineLength")

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.ui.semantics.Role.Companion.Switch
import androidx.compose.ui.semantics.SemanticsProperties.Role
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class GeneratorScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(
        GeneratorState(
            generatedText = "Placeholder",
            selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Password()),
        ),
    )

    private val viewModel = mockk<GeneratorViewModel>(relaxed = true) {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns mutableStateFlow
    }

    @Test
    fun `clicking a MainStateOption should send MainTypeOptionSelect action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescription(label = "What would you like to generate?, Password")
            .performClick()

        // Choose the option from the menu
        composeTestRule.onAllNodesWithText(text = "Password")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.PASSWORD))
        }
    }

    @Test
    fun `clicking a PasscodeOption should send PasscodeTypeOption action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Opens the menu
        composeTestRule.onNodeWithContentDescription(label = "Password, Password").performClick()

        // Choose the option from the menu
        composeTestRule.onAllNodesWithText(text = "Passphrase").onLast().performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
                    GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSPHRASE,
                ),
            )
        }
    }

    @Test
    fun `in Passcode_Passphrase state, decrementing number of words should send NumWordsCounterChange action with decremented value`() {
        val initialNumWords = 3
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Passphrase()),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithContentDescription("Number of words, 3")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange(
                    numWords = initialNumWords - 1,
                ),
            )
        }
    }

    @Test
    fun `in Passcode_Passphrase state, incrementing number of words should send NumWordsCounterChange action with incremented value`() {
        val initialNumWords = 3
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Passphrase()),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithContentDescription("Number of words, 3")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange(
                    numWords = initialNumWords + 1,
                ),
            )
        }
    }

    @Test
    fun `in Passcode_Passphrase state, toggling capitalize should send ToggleCapitalizeChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Passphrase()),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithText("Capitalize")
            .onChildren()
            .filterToOne(expectValue(Role, Switch))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange(
                    capitalize = true,
                ),
            )
        }
    }

    @Test
    fun `in Passcode_Passphrase state, toggling the include number toggle should send ToggleIncludeNumberChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Passphrase()),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Include number")
            .onChildren()
            .filterToOne(expectValue(Role, Switch))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange(
                    includeNumber = true,
                ),
            )
        }
    }

    @Test
    fun `in Passcode_Passphrase state, updating text in word separator should send WordSeparatorTextChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState.MainType.Passcode(GeneratorState.MainType.Passcode.PasscodeType.Passphrase()),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithText("Word separator")
            .performScrollTo()
            .performTextInput("a")

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange(
                    wordSeparator = 'a',
                ),
            )
        }
    }

    private fun updateState(state: GeneratorState) {
        mutableStateFlow.value = state
    }
}
