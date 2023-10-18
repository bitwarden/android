package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GeneratorViewModelTest : BaseViewModelTest() {

    private val initialState = createPasswordState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(initialState)

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `RegenerateClick refreshes the generated text`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        val initialText = viewModel.stateFlow.value.generatedText
        val action = GeneratorAction.RegenerateClick

        viewModel.actionChannel.trySend(action)

        val reversedText = viewModel.stateFlow.value.generatedText
        assertEquals(initialText.reversed(), reversedText)
    }

    @Test
    fun `CopyClick should emit ShowToast`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(GeneratorAction.CopyClick)
            assertEquals(GeneratorEvent.ShowToast("Copied"), awaitItem())
        }
    }

    @Test
    fun `MainTypeOptionSelect PASSWORD should switch to Passcode`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.PASSWORD)

        viewModel.actionChannel.trySend(action)

        val expectedState =
            initialState.copy(selectedType = GeneratorState.MainType.Passcode())

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `MainTypeOptionSelect USERNAME should switch to Username`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.USERNAME)

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(selectedType = GeneratorState.MainType.Username())

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `PasscodeTypeOptionSelect PASSWORD should switch to PasswordType`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        val action = GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
            passcodeTypeOption = GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSWORD,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(
            selectedType = GeneratorState.MainType.Passcode(
                selectedType = GeneratorState.MainType.Passcode.PasscodeType.Password(),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `PasscodeTypeOptionSelect PASSPHRASE should switch to PassphraseType`() = runTest {
        val viewModel = GeneratorViewModel(initialSavedStateHandle)
        val action = GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
            passcodeTypeOption = GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSPHRASE,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(
            selectedType = GeneratorState.MainType.Passcode(
                selectedType = GeneratorState.MainType.Passcode.PasscodeType.Passphrase(),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Nested
    inner class PasswordActions {
        private val defaultPasswordState = createPasswordState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = GeneratorViewModel(initialSavedStateHandle)
        }

        @Test
        fun `SliderLengthChange should update password length correctly to new value`() = runTest {
            viewModel.eventFlow.test {
                val newLength = 16

                viewModel.actionChannel.trySend(
                    GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange(
                        length = newLength,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            length = newLength,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `ToggleCapitalLettersChange should update useCapitals correctly`() = runTest {
            viewModel.eventFlow.test {
                val useCapitals = true

                viewModel.actionChannel.trySend(
                    GeneratorAction
                        .MainType
                        .Passcode
                        .PasscodeType
                        .Password
                        .ToggleCapitalLettersChange(
                            useCapitals = useCapitals,
                        ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useCapitals = useCapitals,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `ToggleLowercaseLettersChange should update useLowercase correctly`() = runTest {
            viewModel.eventFlow.test {
                val useLowercase = true

                viewModel.actionChannel.trySend(
                    GeneratorAction
                        .MainType
                        .Passcode
                        .PasscodeType
                        .Password
                        .ToggleLowercaseLettersChange(
                            useLowercase = useLowercase,
                        ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useLowercase = useLowercase,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `ToggleNumbersChange should update useNumbers correctly`() = runTest {
            viewModel.eventFlow.test {
                val useNumbers = true

                viewModel.actionChannel.trySend(
                    GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange(
                        useNumbers = useNumbers,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useNumbers = useNumbers,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `ToggleSpecialCharactersChange should update useSpecialChars correctly`() = runTest {
            viewModel.eventFlow.test {
                val useSpecialChars = true

                viewModel.actionChannel.trySend(
                    GeneratorAction
                        .MainType
                        .Passcode
                        .PasscodeType
                        .Password
                        .ToggleSpecialCharactersChange(
                            useSpecialChars = useSpecialChars,
                        ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useSpecialChars = useSpecialChars,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `MinNumbersCounterChange should update minNumbers correctly`() = runTest {
            viewModel.eventFlow.test {
                val minNumbers = 4

                viewModel.actionChannel.trySend(
                    GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange(
                        minNumbers = minNumbers,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            minNumbers = minNumbers,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `MinSpecialCharactersChange should update minSpecial correctly`() = runTest {
            viewModel.eventFlow.test {
                val minSpecial = 2

                viewModel.actionChannel.trySend(
                    GeneratorAction
                        .MainType
                        .Passcode
                        .PasscodeType
                        .Password
                        .MinSpecialCharactersChange(
                            minSpecial = minSpecial,
                        ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = "redlohecalP",
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            minSpecial = minSpecial,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
        }

        @Test
        fun `ToggleAvoidAmbigousCharactersChange should update avoidAmbiguousChars correctly`() =
            runTest {
                viewModel.eventFlow.test {
                    val avoidAmbiguousChars = true

                    viewModel.actionChannel.trySend(
                        GeneratorAction
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Password
                            .ToggleAvoidAmbigousCharactersChange(
                                avoidAmbiguousChars = avoidAmbiguousChars,
                            ),
                    )

                    val expectedState = defaultPasswordState.copy(
                        generatedText = "redlohecalP",
                        selectedType = GeneratorState.MainType.Passcode(
                            GeneratorState.MainType.Passcode.PasscodeType.Password(
                                avoidAmbiguousChars = avoidAmbiguousChars,
                            ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
            }
    }

    @Nested
    inner class PassphraseActions {

        private val defaultPassphraseState = createPassphraseState()
        private val passphraseSavedStateHandle =
            createSavedStateHandleWithState(defaultPassphraseState)

        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            viewModel = GeneratorViewModel(passphraseSavedStateHandle)
        }

        @Test
        fun `NumWordsCounterChange should update the numWords property correctly`() =
            runTest {
                viewModel.eventFlow.test {
                    val newNumWords = 4
                    viewModel.actionChannel.trySend(
                        GeneratorAction
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase
                            .NumWordsCounterChange(
                                numWords = newNumWords,
                            ),
                    )

                    val expectedState = defaultPassphraseState.copy(
                        generatedText = "redlohecalP",
                        selectedType = GeneratorState.MainType.Passcode(
                            GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                                numWords = newNumWords,
                            ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
            }

        @Test
        fun `WordSeparatorTextChange should update wordSeparator correctly to new value`() =
            runTest {
                viewModel.eventFlow.test {
                    val newWordSeparatorChar = '_'

                    viewModel.actionChannel.trySend(
                        GeneratorAction
                            .MainType
                            .Passcode
                            .PasscodeType.Passphrase
                            .WordSeparatorTextChange(
                                wordSeparator = newWordSeparatorChar,
                            ),
                    )

                    val expectedState = defaultPassphraseState.copy(
                        generatedText = "redlohecalP",
                        selectedType = GeneratorState.MainType.Passcode(
                            GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                                wordSeparator = newWordSeparatorChar,
                            ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
            }

        @Test
        fun `ToggleIncludeNumberChange should update the includeNumber property correctly`() =
            runTest {
                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        GeneratorAction
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase
                            .ToggleIncludeNumberChange(
                                includeNumber = true,
                            ),
                    )

                    val expectedState = defaultPassphraseState.copy(
                        generatedText = "redlohecalP",
                        selectedType = GeneratorState.MainType.Passcode(
                            selectedType = GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                                includeNumber = true,
                            ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
            }

        @Test
        fun `ToggleCapitalizeChange should update the capitalize property correctly`() =
            runTest {
                viewModel.eventFlow.test {
                    viewModel.actionChannel.trySend(
                        GeneratorAction
                            .MainType
                            .Passcode
                            .PasscodeType
                            .Passphrase
                            .ToggleCapitalizeChange(
                                capitalize = true,
                            ),
                    )

                    val expectedState = defaultPassphraseState.copy(
                        generatedText = "redlohecalP",
                        selectedType = GeneratorState.MainType.Passcode(
                            GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                                capitalize = true,
                            ),
                        ),
                    )

                    assertEquals(expectedState, viewModel.stateFlow.value)
                }
            }
    }
    //region Helper Functions

    @Suppress("LongParameterList")
    private fun createPasswordState(
        generatedText: String = "Placeholder",
        length: Int = 14,
        useCapitals: Boolean = true,
        useLowercase: Boolean = true,
        useNumbers: Boolean = true,
        useSpecialChars: Boolean = false,
        minNumbers: Int = 1,
        minSpecial: Int = 1,
        avoidAmbiguousChars: Boolean = false,
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Passcode(
                GeneratorState.MainType.Passcode.PasscodeType.Password(
                    length = length,
                    useCapitals = useCapitals,
                    useLowercase = useLowercase,
                    useNumbers = useNumbers,
                    useSpecialChars = useSpecialChars,
                    minNumbers = minNumbers,
                    minSpecial = minSpecial,
                    avoidAmbiguousChars = avoidAmbiguousChars,
                ),
            ),
        )

    private fun createPassphraseState(
        generatedText: String = "Placeholder",
        numWords: Int = 3,
        wordSeparator: Char = '-',
        capitalize: Boolean = false,
        includeNumber: Boolean = false,
    ): GeneratorState =
        GeneratorState(
            generatedText = generatedText,
            selectedType = GeneratorState.MainType.Passcode(
                GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                    numWords = numWords,
                    wordSeparator = wordSeparator,
                    capitalize = capitalize,
                    includeNumber = includeNumber,
                ),
            ),
        )

    private fun createSavedStateHandleWithState(state: GeneratorState) =
        SavedStateHandle().apply {
            set("state", state)
        }

    //endregion Helper Functions
}
