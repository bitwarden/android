package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.AnnotatedString
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

@Suppress("LargeClass")
class GeneratorScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(
        GeneratorState(
            generatedText = "Placeholder",
            selectedType = GeneratorState
                .MainType
                .Passcode(
                    GeneratorState
                        .MainType
                        .Passcode
                        .PasscodeType
                        .Password(),
                ),
        ),
    )

    private val viewModel = mockk<GeneratorViewModel>(relaxed = true) {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns mutableStateFlow
    }

    @Test
    fun `clicking the Regenerate button should send RegenerateClick action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Generate password")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.RegenerateClick)
        }
    }

    @Test
    fun `clicking the Copy button should send CopyClick action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Copy")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.CopyClick)
        }
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
        composeTestRule
            .onAllNodesWithText(text = "Password")
            .onLast()
            .performScrollTo()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.PASSWORD),
            )
        }

        // Make sure dialog is hidden:
        composeTestRule
            .onNode(isDialog())
            .assertDoesNotExist()
    }

    @Test
    fun `clicking a PasscodeOption should send PasscodeTypeOption action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescription(label = "Password type, Password")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Passphrase")
            .onLast()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
                    GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSPHRASE,
                ),
            )
        }

        // Make sure dialog is hidden:
        composeTestRule
            .onNode(isDialog())
            .assertDoesNotExist()
    }

    //region Passcode Password Tests

    @Test
    fun `in Passcode_Password state, the ViewModel state should update the UI correctly`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithContentDescription(label = "What would you like to generate?, Password")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(label = "Password type, Password")
            .assertIsDisplayed()

        composeTestRule
            .onNode(
                expectValue(
                    SemanticsProperties.EditableText, AnnotatedString("14"),
                ),
            )
            .assertExists()

        composeTestRule
            .onNodeWithText("A—Z")
            .performScrollTo()
            .assertIsOn()

        composeTestRule
            .onNodeWithText("a—z")
            .performScrollTo()
            .assertIsOn()

        composeTestRule
            .onNodeWithText("0-9")
            .performScrollTo()
            .assertIsOn()

        composeTestRule
            .onNodeWithText("!@#$%^&*")
            .performScrollTo()
            .assertIsOff()

        composeTestRule
            .onNodeWithContentDescription("Minimum numbers, 1")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Minimum numbers, 1")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Avoid ambiguous characters")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, adjusting the slider should send SliderLengthChange action with length not equal to default`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithText("Length")
            .onSiblings()
            .filterToOne(
                hasProgressBarRangeInfo(
                    ProgressBarRangeInfo(
                        current = 13.6484375f,
                        range = 5.0f..128.0f,
                        steps = 127,
                    ),
                ),
            )
            .performScrollTo()
            .performTouchInput {
                swipeRight(50f, 800f)
            }

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange(
                    length = 128,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, toggling the capital letters toggle should send ToggleCapitalLettersChange action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("A—Z")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleCapitalLettersChange(
                    useCapitals = false,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, toggling the use lowercase toggle should send ToggleLowercaseLettersChange action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("a—z")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Passcode
                    .PasscodeType
                    .Password
                    .ToggleLowercaseLettersChange(
                        useLowercase = false,
                    ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, toggling the use numbers toggle should send ToggleNumbersChange action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("0-9")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange(
                    useNumbers = false,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, toggling the use special characters toggle should send ToggleSpecialCharactersChange action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("!@#$%^&*")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Passcode
                    .PasscodeType
                    .Password
                    .ToggleSpecialCharactersChange(
                        useSpecialChars = true,
                    ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, decrementing the minimum numbers counter should send MinNumbersCounterChange action`() {
        val initialMinNumbers = 1
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum numbers, 1")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange(
                    minNumbers = initialMinNumbers - 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, incrementing the minimum numbers counter should send MinNumbersCounterChange action`() {
        val initialMinNumbers = 1
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum numbers, 1")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange(
                    minNumbers = initialMinNumbers + 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, decrementing the minimum numbers counter below 0 should do nothing`() {
        val initialMinNumbers = 0
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(minNumbers = initialMinNumbers),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum numbers, $initialMinNumbers")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, incrementing the minimum numbers counter above 5 should do nothing`() {
        val initialMinNumbers = 5
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(minNumbers = initialMinNumbers),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum numbers, $initialMinNumbers")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, decrementing the minimum special characters counter should send MinSpecialCharactersChange action`() {
        val initialSpecialChars = 1
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum special, 1")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinSpecialCharactersChange(
                    minSpecial = initialSpecialChars - 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, incrementing the minimum special characters counter should send MinSpecialCharactersChange action`() {
        val initialSpecialChars = 1
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum special, 1")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinSpecialCharactersChange(
                    minSpecial = initialSpecialChars + 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, decrementing the minimum special characters below 0 should do nothing`() {
        val initialSpecialChars = 0
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(minSpecial = initialSpecialChars),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum special, $initialSpecialChars")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, decrementing the minimum special characters above 5 should do nothing`() {
        val initialSpecialChars = 5
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password(minSpecial = initialSpecialChars),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Minimum special, $initialSpecialChars")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Password state, toggling the use avoid ambiguous characters toggle should send ToggleSpecialCharactersChange action`() {
        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Avoid ambiguous characters")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Passcode
                    .PasscodeType
                    .Password
                    .ToggleAvoidAmbigousCharactersChange(
                        avoidAmbiguousChars = true,
                    ),
            )
        }
    }

    //endregion Passcode Password Tests

    //region Passcode Passphrase Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Passphrase state, decrementing number of words should send NumWordsCounterChange action with decremented value`() {
        val initialNumWords = 4
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(numWords = initialNumWords),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithContentDescription("Number of words, $initialNumWords")
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
    fun `in Passcode_Passphrase state, decrementing number of words under 3 should do nothing`() {
        val initialNumWords = 3
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(numWords = initialNumWords),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithContentDescription("Number of words, $initialNumWords")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()
        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Test
    fun `in Passcode_Passphrase state, incrementing number of words over 20 should do nothing`() {
        val initialNumWords = 20
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(numWords = initialNumWords),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithContentDescription("Number of words, $initialNumWords")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()
        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Passphrase state, incrementing number of words should send NumWordsCounterChange action with incremented value`() {
        val initialNumWords = 3
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(),
                    ),
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

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Passphrase state, toggling capitalize should send ToggleCapitalizeChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule
            .onNodeWithText("Capitalize")
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

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Passphrase state, toggling the include number toggle should send ToggleIncludeNumberChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(),
                    ),
            ),
        )

        composeTestRule.setContent {
            GeneratorScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Include number")
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

    @Suppress("MaxLineLength")
    @Test
    fun `in Passcode_Passphrase state, updating text in word separator should send WordSeparatorTextChange action`() {
        updateState(
            GeneratorState(
                generatedText = "Placeholder",
                selectedType = GeneratorState
                    .MainType
                    .Passcode(
                        GeneratorState
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase(),
                    ),
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

    //endregion Passcode Passphrase Tests

    private fun updateState(state: GeneratorState) {
        mutableStateFlow.value = state
    }
}
