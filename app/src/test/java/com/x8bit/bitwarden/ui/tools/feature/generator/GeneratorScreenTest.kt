package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
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
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

@Suppress("LargeClass")
class GeneratorScreenTest : BaseComposeTest() {
    private var onNavigateToPasswordHistoryScreenCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val mutableEventFlow = bufferedMutableSharedFlow<GeneratorEvent>()
    private val viewModel = mockk<GeneratorViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { launchUri(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            GeneratorScreen(
                viewModel = viewModel,
                onNavigateToPasswordHistory = { onNavigateToPasswordHistoryScreenCalled = true },
                onNavigateBack = {},
                intentManager = intentManager,
            )
        }
    }

    @Test
    fun `ModalAppBar should be displayed for Password Mode`() {
        updateState(DEFAULT_STATE.copy(generatorMode = GeneratorMode.Modal.Password))

        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Select")
            .assertIsDisplayed()
    }

    @Test
    fun `ModalAppBar should be displayed for Username Mode`() {
        updateState(
            DEFAULT_STATE.copy(
                generatorMode = GeneratorMode.Modal.Username(website = null),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Select")
            .assertIsDisplayed()
    }

    @Test
    fun `on close click should send CloseClick`() {
        updateState(
            DEFAULT_STATE.copy(
                generatorMode = GeneratorMode.Modal.Username(website = null),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.CloseClick)
        }
    }

    @Test
    fun `on select click should send SelectClick`() {
        updateState(
            DEFAULT_STATE.copy(
                generatorMode = GeneratorMode.Modal.Username(website = null),
            ),
        )

        composeTestRule
            .onNodeWithText(text = "Select")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.SelectClick)
        }
    }

    @Test
    fun `DefaultAppBar should be displayed for Default Mode`() {
        updateState(DEFAULT_STATE.copy(generatorMode = GeneratorMode.Default))

        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .assertIsDisplayed()
    }

    @Test
    fun `MainTypeOption select control should be hidden for password mode`() {
        updateState(DEFAULT_STATE.copy(generatorMode = GeneratorMode.Modal.Password))

        composeTestRule
            .onNodeWithContentDescription(label = "What would you like to generate?, Password")
            .assertDoesNotExist()
    }

    @Test
    fun `MainTypeOption select control should be hidden for username mode`() {
        updateState(
            DEFAULT_STATE.copy(
                generatorMode = GeneratorMode.Modal.Username(website = null),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription(label = "What would you like to generate?, Password")
            .assertDoesNotExist()
    }

    @Test
    fun `NavigateToPasswordHistory event should call onNavigateToPasswordHistoryScreen`() {
        mutableEventFlow.tryEmit(GeneratorEvent.NavigateToPasswordHistory)
        assertTrue(onNavigateToPasswordHistoryScreenCalled)
    }

    @Test
    fun `Snackbar should be displayed with correct message on ShowSnackbar event`() {
        mutableEventFlow.tryEmit(GeneratorEvent.ShowSnackbar("Test Snackbar Message".asText()))

        composeTestRule
            .onNodeWithText("Test Snackbar Message")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking the Regenerate button should send RegenerateClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Generate password")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.RegenerateClick)
        }
    }

    @Test
    fun `clicking the Copy button should send CopyClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Copy")
            .performClick()

        verify {
            viewModel.trySendAction(GeneratorAction.CopyClick)
        }
    }

    @Test
    fun `clicking a MainStateOption should send MainTypeOptionSelect action`() {
        // Opens the menu
        composeTestRule
            .onNodeWithText(text = "Password")
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

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a UsernameOption should send UsernameTypeOption action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(),
                ),
            ),
        )

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescription(
                label = "Plus addressed email. Username type. Use your email provider's subaddress capabilities",
            )
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Random word")
            .onLast()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                    GeneratorState.MainType.Username.UsernameTypeOption.RANDOM_WORD,
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
    fun `in Password state, the ViewModel state should update the UI correctly`() {
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
            .onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "1")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "1")
            .onSiblings()
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
    fun `in Password state, adjusting the slider should send SliderLengthChange action with length not equal to default`() {
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
                GeneratorAction.MainType.Password.SliderLengthChange(
                    length = 128,
                    isUserInteracting = true,
                ),
            )
        }

        // This value would be 128 in a real scenario, because length passed here depends on the
        // internal length which is indirectly updated via the call verified above. However, because
        // the view model is a mock, the length value that internal value depends on will not
        // actually get updated from its original value, and thus will be its original value of 14.
        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.SliderLengthChange(
                    length = 14,
                    isUserInteracting = false,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, toggling the capital letters toggle should send ToggleCapitalLettersChange action`() {
        composeTestRule.onNodeWithText("A—Z")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleCapitalLettersChange(
                    useCapitals = false,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, toggling the use lowercase toggle should send ToggleLowercaseLettersChange action`() {
        composeTestRule.onNodeWithText("a—z")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                    useLowercase = false,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, toggling the use numbers toggle should send ToggleNumbersChange action`() {
        composeTestRule.onNodeWithText("0-9")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleNumbersChange(useNumbers = false),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, toggling the use special characters toggle should send ToggleSpecialCharactersChange action`() {
        composeTestRule.onNodeWithText("!@#$%^&*")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                    useSpecialChars = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, decrementing the minimum numbers counter should send MinNumbersCounterChange action`() {
        val initialMinNumbers = 1

        composeTestRule.onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "1")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinNumbersCounterChange(
                    minNumbers = initialMinNumbers - 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, incrementing the minimum numbers counter should send MinNumbersCounterChange action`() {
        val initialMinNumbers = 1

        composeTestRule.onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "1")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinNumbersCounterChange(
                    minNumbers = initialMinNumbers + 1,
                ),
            )
        }
    }

    @Test
    fun `in Password state, decrementing the minimum numbers counter below 0 should do nothing`() {
        val initialMinNumbers = 0
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(minNumbers = initialMinNumbers),
            ),
        )

        composeTestRule.onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "$initialMinNumbers")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Test
    fun `in Password state, incrementing the minimum numbers counter above 9 should do nothing`() {
        val initialMinNumbers = 9
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(minNumbers = initialMinNumbers),
            ),
        )

        composeTestRule.onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "$initialMinNumbers")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, decrementing the minimum special characters counter should send MinSpecialCharactersChange action`() {
        val initialSpecialChars = 1

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "1")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinSpecialCharactersChange(
                    minSpecial = initialSpecialChars - 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, incrementing the minimum special characters counter should send MinSpecialCharactersChange action`() {
        val initialSpecialChars = 1

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "1")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinSpecialCharactersChange(
                    minSpecial = initialSpecialChars + 1,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, decrementing the minimum special characters below 0 should do nothing`() {
        val initialSpecialChars = 0
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(minSpecial = initialSpecialChars),
            ),
        )

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "$initialSpecialChars")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()
        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, decrementing the minimum special characters above 9 should do nothing`() {
        val initialSpecialChars = 9
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(minSpecial = initialSpecialChars),
            ),
        )

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "$initialSpecialChars")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()
        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Password state, toggling the use avoid ambiguous characters toggle should send ToggleSpecialCharactersChange action`() {
        composeTestRule.onNodeWithText("Avoid ambiguous characters")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange(
                    avoidAmbiguousChars = true,
                ),
            )
        }
    }

    @Test
    fun `in Password state, disabled elements should not send events`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(
                    capitalsEnabled = false,
                    lowercaseEnabled = false,
                    numbersEnabled = false,
                    specialCharsEnabled = false,
                    ambiguousCharsEnabled = false,
                ),
            ),
        )

        composeTestRule.onNodeWithText("A—Z")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("a—z")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("0-9")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("!@#$%^&*")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Avoid ambiguous characters")
            .performScrollTo()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleCapitalLettersChange(useCapitals = false),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleLowercaseLettersChange(
                    useLowercase = false,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleNumbersChange(useNumbers = false),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleSpecialCharactersChange(
                    useSpecialChars = true,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.ToggleAvoidAmbiguousCharactersChange(
                    avoidAmbiguousChars = true,
                ),
            )
        }
    }

    @Test
    fun `in Password state, minimum numbers cannot go below minimum threshold`() {
        val initialMinNumbers = 5

        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(
                    minNumbersAllowed = initialMinNumbers,
                ),
            ),
        )

        composeTestRule.onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "5")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinNumbersCounterChange(
                    minNumbers = 4,
                ),
            )
        }
    }

    @Test
    fun `in Password state, maximum numbers should match minimum if lower`() {
        val initialMinNumbers = 7
        val initialMaxNumbers = 5

        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(
                    minNumbersAllowed = initialMinNumbers,
                    maxNumbersAllowed = initialMaxNumbers,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Minimum numbers")
            .assertTextEquals("Minimum numbers", "7")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `in Password state, minimum special characters cannot go below minimum threshold`() {
        val initialMinSpecials = 5

        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(
                    minSpecialAllowed = initialMinSpecials,
                ),
            ),
        )

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "5")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                GeneratorAction.MainType.Password.MinSpecialCharactersChange(
                    minSpecial = 4,
                ),
            )
        }
    }

    @Test
    fun `in Password state, maximum special should match minimum if lower `() {
        val initialMinSpecials = 7
        val initialMaxSpecials = 5

        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Password(
                    minSpecialAllowed = initialMinSpecials,
                    maxSpecialAllowed = initialMaxSpecials,
                ),
            ),
        )

        composeTestRule.onNodeWithText("Minimum special")
            .assertTextEquals("Minimum special", "7")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()
    }

    @Test
    fun `in Passphrase state, disabled elements should not send events`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(
                    capitalizeEnabled = false,
                    includeNumberEnabled = false,
                ),
            ),
        )

        composeTestRule.onNodeWithText("Capitalize")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Include number")
            .performScrollTo()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange(
                    capitalize = true,
                ),
            )
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange(
                    includeNumber = true,
                ),
            )
        }
    }

    @Test
    fun `in Passphrase state, minimum number of words cannot go below minimum threshold`() {
        val initialMinWords = 5

        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(minNumWords = initialMinWords),
            ),
        )

        composeTestRule.onNodeWithText("Number of words")
            .assertTextEquals("Number of words", "5")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.NumWordsCounterChange(numWords = 4),
            )
        }
    }

    //endregion Passcode Password Tests

    //region Passcode Passphrase Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Passphrase state, decrementing number of words should send NumWordsCounterChange action with decremented value`() {
        val initialNumWords = 4
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(numWords = initialNumWords),
            ),
        )

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithText("Number of words")
            .assertTextEquals("Number of words", "$initialNumWords")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.NumWordsCounterChange(
                    numWords = initialNumWords - 1,
                ),
            )
        }
    }

    @Test
    fun `in Passphrase state, decrementing number of words under 3 should do nothing`() {
        val initialNumWords = 3
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(numWords = initialNumWords),
            ),
        )

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithText("Number of words")
            .assertTextEquals("Number of words", "$initialNumWords")
            .onSiblings()
            .filterToOne(hasContentDescription("\u2212"))
            .performScrollTo()
            .performClick()
        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Test
    fun `in Passphrase state, incrementing number of words over 20 should do nothing`() {
        val initialNumWords = 20
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(numWords = initialNumWords),
            ),
        )

        // Unicode for "minus" used for content description
        composeTestRule
            .onNodeWithText("Number of words")
            .assertTextEquals("Number of words", "$initialNumWords")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()
        verify(exactly = 1) { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
        verify(exactly = 1) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passphrase state, incrementing number of words should send NumWordsCounterChange action with incremented value`() {
        val initialNumWords = 3
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(),
            ),
        )

        composeTestRule
            .onNodeWithText("Number of words")
            .assertTextEquals("Number of words", "3")
            .onSiblings()
            .filterToOne(hasContentDescription("+"))
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.NumWordsCounterChange(
                    numWords = initialNumWords + 1,
                ),
            )
        }
    }

    @Test
    fun `in Passphrase state, toggling capitalize should send ToggleCapitalizeChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(),
            ),
        )

        composeTestRule
            .onNodeWithText("Capitalize")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.ToggleCapitalizeChange(
                    capitalize = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passphrase state, toggling the include number toggle should send ToggleIncludeNumberChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(),
            ),
        )

        composeTestRule.onNodeWithText("Include number")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.ToggleIncludeNumberChange(
                    includeNumber = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Passphrase state, updating text in word separator should send WordSeparatorTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Passphrase(),
            ),
        )

        composeTestRule
            .onNodeWithText("Word separator")
            .performScrollTo()
            .performTextInput("a")

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Passphrase.WordSeparatorTextChange(
                    wordSeparator = 'a',
                ),
            )
        }
    }

    //endregion Passcode Passphrase Tests

    //region Forwarded Email Alias Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias state, updating the service type should send ServiceTypeOptionSelect action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(),
                ),
            ),
        )

        val newServiceType = GeneratorState
            .MainType
            .Username
            .UsernameType
            .ForwardedEmailAlias
            .ServiceTypeOption
            .ADDY_IO

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescription(label = "null. Service")
            .performScrollTo()
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "addy.io")
            .onLast()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceTypeOptionSelect(
                    newServiceType,
                ),
            )
        }

        // Make sure dialog is hidden:
        composeTestRule
            .onNode(isDialog())
            .assertDoesNotExist()
    }

    //endregion Forwarded Email Alias Tests

    //region Addy.Io Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_AddyIo state, updating access token text input should send AccessTokenTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .AddyIo(),
                    ),
                ),
            ),
        )

        val newAccessToken = "accessToken"

        composeTestRule
            .onNodeWithText("API access token")
            .performScrollTo()
            .performTextInput(newAccessToken)

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .ForwardedEmailAlias
                    .AddyIo
                    .AccessTokenTextChange(
                        accessToken = newAccessToken,
                    ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_AddyIo state, updating domain name text input should send DomainTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .AddyIo(),
                    ),
                ),
            ),
        )

        val newDomainName = "domainName"

        composeTestRule
            .onNodeWithText("Domain name (required)")
            .performScrollTo()
            .performTextInput(newDomainName)

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .ForwardedEmailAlias
                    .AddyIo
                    .DomainTextChange(
                        domain = newDomainName,
                    ),
            )
        }
    }

    //endregion Addy.Io Service Type Tests

    //region DuckDuckGo Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_DuckDuckGo state, updating api key text input should send ApiKeyTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .DuckDuckGo(),
                    ),
                ),
            ),
        )

        val newApiKey = "apiKey"

        composeTestRule
            .onNodeWithText("API key (required)")
            .performScrollTo()
            .performTextInput(newApiKey)

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.DuckDuckGo.ApiKeyTextChange(
                    apiKey = newApiKey,
                ),
            )
        }
    }

    //endregion DuckDuckGo Service Type Tests

    //region FastMail Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_FastMail state, updating api key text input should send ApiKeyTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .FastMail(),
                    ),
                ),
            ),
        )

        val newApiKey = "apiKey"

        composeTestRule
            .onNodeWithText("API key (required)")
            .performScrollTo()
            .performTextInput(newApiKey)

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.FastMail.ApiKeyTextChange(
                    apiKey = newApiKey,
                ),
            )
        }
    }

    //endregion FastMail Service Type Tests

    //region FirefoxRelay Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_FirefoxRelay state, updating access token text input should send AccessTokenTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .FirefoxRelay(),
                    ),
                ),
            ),
        )

        val newAccessToken = "accessToken"

        composeTestRule
            .onNodeWithText("API access token")
            .performScrollTo()
            .performTextInput(newAccessToken)

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .ForwardedEmailAlias
                    .FirefoxRelay
                    .AccessTokenTextChange(
                        accessToken = newAccessToken,
                    ),
            )
        }
    }

    //endregion FirefoxRelay Service Type Tests

    //region SimpleLogin Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_SimpleLogin state, updating api key text input should send ApiKeyTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .SimpleLogin(),
                    ),
                ),
            ),
        )

        val newApiKey = "apiKey"

        composeTestRule
            .onNodeWithText("API key (required)")
            .performScrollTo()
            .performTextInput(newApiKey)

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias.SimpleLogin.ApiKeyTextChange(
                    apiKey = newApiKey,
                ),
            )
        }
    }

    //endregion SimpleLogin Service Type Tests

    //region ForwardEmail Service Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_ForwardEmail state, updating api key text input should send ApiKeyTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .ForwardEmail(),
                    ),
                ),
            ),
        )

        val newApiKey = "apiKey"

        composeTestRule
            .onNodeWithText("API key (required)")
            .performScrollTo()
            .performTextInput(newApiKey)

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .ForwardedEmailAlias
                    .ForwardEmail
                    .ApiKeyTextChange(
                        apiKey = newApiKey,
                    ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_ForwardedEmailAlias_ForwardEmail state, updating domain name text input should send DomainNameChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias(
                        selectedServiceType = GeneratorState
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceType
                            .ForwardEmail(),
                    ),
                ),
            ),
        )

        val newDomainName = "domainName"

        composeTestRule
            .onNodeWithText("Domain name (required)")
            .performScrollTo()
            .performTextInput(newDomainName)

        verify {
            viewModel.trySendAction(
                GeneratorAction
                    .MainType
                    .Username
                    .UsernameType
                    .ForwardedEmailAlias
                    .ForwardEmail
                    .DomainNameTextChange(
                        domainName = newDomainName,
                    ),
            )
        }
    }

    //endregion ForwardEmail Service Type Tests

    //region Username Type Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username state, clicking the tooltip icon should send the TooltipClick action`() {
        updateState(DEFAULT_STATE.copy(selectedType = GeneratorState.MainType.Username()))

        composeTestRule
            .onNodeWithContentDescription(
                label = "Plus addressed email. Username type. Use your email provider's subaddress capabilities",
                useUnmergedTree = true,
            )
            // Find the button
            .onChildren()
            .filterToOne(hasClickAction())
            // Find the content description
            .onChildren()
            .filterToOne(hasContentDescription("Learn more"))
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.TooltipClick,
            )
        }
    }

    @Test
    fun `on NavigateToTooltip should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(GeneratorEvent.NavigateToTooltip)
        verify {
            intentManager.launchUri("https://bitwarden.com/help/generator/#username-types".toUri())
        }
    }

    //endregion Username Type Tests

    //region Username Plus Addressed Email Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_PlusAddressedEmail state, updating text in email field should send EmailTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail(),
                ),
            ),
        )

        val newEmail = "test@example.com"

        // Find the text field for PlusAddressedEmail and input text
        composeTestRule
            .onNodeWithText("Email (required)")
            .performScrollTo()
            .performTextInput(newEmail)

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.PlusAddressedEmail.EmailTextChange(
                    email = newEmail,
                ),
            )
        }
    }

    //endregion Username Plus Addressed Email Tests

    //region Catch-All Email Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_CatchAllEmail state, updating text in email field should send EmailTextChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.CatchAllEmail(),
                ),
            ),
        )

        val newDomain = "test.com"

        // Find the text field for Catch-All Email and input text
        composeTestRule
            .onNodeWithText("Domain name (required)")
            .performScrollTo()
            .performTextInput(newDomain)

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.CatchAllEmail.DomainTextChange(
                    domain = newDomain,
                ),
            )
        }
    }

    //endregion Catch-All Email Tests

    //region Random Word Tests

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_RandomWord state, toggling capitalize should send ToggleCapitalizeChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.RandomWord(),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Capitalize")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.RandomWord.ToggleCapitalizeChange(
                    capitalize = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in Username_RandomWord state, toggling the include number toggle should send ToggleIncludeNumberChange action`() {
        updateState(
            DEFAULT_STATE.copy(
                selectedType = GeneratorState.MainType.Username(
                    GeneratorState.MainType.Username.UsernameType.RandomWord(),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Include number")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                GeneratorAction.MainType.Username.UsernameType.RandomWord.ToggleIncludeNumberChange(
                    includeNumber = true,
                ),
            )
        }
    }

    @Test
    fun `send LifecycleResumed action on screen resume`() {
        verify { viewModel.trySendAction(GeneratorAction.LifecycleResume) }
    }

    //endregion Random Word Tests

    private fun updateState(state: GeneratorState) {
        mutableStateFlow.value = state
    }
}

private val DEFAULT_STATE = GeneratorState(
    generatedText = "",
    selectedType = GeneratorState.MainType.Password(),
    currentEmailAddress = "currentEmail",
)
