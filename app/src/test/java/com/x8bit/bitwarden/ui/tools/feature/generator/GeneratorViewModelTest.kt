package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GeneratorViewModelTest : BaseViewModelTest() {

    private val initialState = createPasswordState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(initialState)

    private val initialPassphraseState = createPassphraseState()
    private val passphraseSavedStateHandle = createSavedStateHandleWithState(initialPassphraseState)

    private val initialUsernameState = createUsernameState()
    private val usernameSavedStateHandle = createSavedStateHandleWithState(initialUsernameState)

    private val fakeGeneratorRepository = FakeGeneratorRepository().apply {
        setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("defaultPassword"),
        )
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for password state updates generatedText and saves password generation options on successful password generation`() =
        runTest {
            val updatedGeneratedPassword = "updatedPassword"

            val viewModel = createViewModel()
            val initialState = viewModel.stateFlow.value

            val updatedPasswordOptions = PasscodeGenerationOptions(
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = true,
                minNumber = 1,
                hasUppercase = true,
                minUppercase = null,
                hasLowercase = true,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 1,
                allowCapitalize = false,
                allowIncludeNumber = false,
                wordSeparator = "-",
                numWords = 3,
            )

            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success(updatedGeneratedPassword),
            )

            viewModel.actionChannel.trySend(GeneratorAction.RegenerateClick)

            val expectedState = initialState.copy(generatedText = updatedGeneratedPassword)
            assertEquals(expectedState, viewModel.stateFlow.value)

            assertEquals(
                updatedPasswordOptions,
                fakeGeneratorRepository.getPasscodeGenerationOptions(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for password state sends ShowSnackbar event on password generation failure`() =
        runTest {
            val viewModel = createViewModel()

            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.InvalidRequest,
            )

            viewModel.actionChannel.trySend(GeneratorAction.RegenerateClick)

            viewModel.eventFlow.test {
                assertEquals(
                    GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for passphrase state updates generatedText and saves passphrase generation options on successful passphrase generation`() =
        runTest {
            val updatedGeneratedPassphrase = "updatedPassphrase"

            val viewModel = createViewModel(initialPassphraseState)
            val initialState = viewModel.stateFlow.value

            val updatedPassphraseOptions = PasscodeGenerationOptions(
                length = 14,
                allowAmbiguousChar = false,
                hasNumbers = true,
                minNumber = 1,
                hasUppercase = true,
                minUppercase = null,
                hasLowercase = true,
                minLowercase = null,
                allowSpecial = false,
                minSpecial = 1,
                allowCapitalize = false,
                allowIncludeNumber = false,
                wordSeparator = "-",
                numWords = 3,
            )

            fakeGeneratorRepository.setMockGeneratePassphraseResult(
                GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
            )

            viewModel.actionChannel.trySend(GeneratorAction.RegenerateClick)

            val expectedState = initialState.copy(generatedText = updatedGeneratedPassphrase)
            assertEquals(expectedState, viewModel.stateFlow.value)

            assertEquals(
                updatedPassphraseOptions,
                fakeGeneratorRepository.getPasscodeGenerationOptions(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `RegenerateClick action for passphrase state sends ShowSnackbar event on passphrase generation failure`() =
        runTest {
            val viewModel = createViewModel(initialPassphraseState)

            fakeGeneratorRepository.setMockGeneratePassphraseResult(
                GeneratedPassphraseResult.InvalidRequest,
            )

            viewModel.actionChannel.trySend(GeneratorAction.RegenerateClick)

            viewModel.eventFlow.test {
                assertEquals(
                    GeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `RegenerateClick for username state should do nothing`() = runTest {
        val viewModel = GeneratorViewModel(usernameSavedStateHandle, fakeGeneratorRepository)

        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("DifferentUsername"),
        )

        viewModel.actionChannel.trySend(GeneratorAction.RegenerateClick)

        assertEquals(initialUsernameState, viewModel.stateFlow.value)
    }

    @Test
    fun `CopyClick should emit CopyTextToClipboard event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(GeneratorAction.CopyClick)

            assertEquals(GeneratorEvent.CopyTextToClipboard, awaitItem())
        }
    }

    @Test
    fun `MainTypeOptionSelect PASSWORD should switch to Passcode`() = runTest {
        val viewModel = createViewModel()
        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("updatedText"),
        )

        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.PASSWORD)

        viewModel.actionChannel.trySend(action)

        val expectedState =
            initialState.copy(
                selectedType = GeneratorState.MainType.Passcode(),
                generatedText = "updatedText",
            )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `MainTypeOptionSelect USERNAME should switch to Username`() = runTest {
        val viewModel = createViewModel()
        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("updatedText"),
        )

        val action = GeneratorAction.MainTypeOptionSelect(GeneratorState.MainTypeOption.USERNAME)

        viewModel.actionChannel.trySend(action)

        val expectedState =
            initialState.copy(selectedType = GeneratorState.MainType.Username())

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `PasscodeTypeOptionSelect PASSWORD should switch to PasswordType`() = runTest {
        val viewModel = createViewModel()
        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success("updatedText"),
        )

        val action = GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
            passcodeTypeOption = GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSWORD,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(
            selectedType = GeneratorState.MainType.Passcode(
                selectedType = GeneratorState.MainType.Passcode.PasscodeType.Password(),
            ),
            generatedText = "updatedText",
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `PasscodeTypeOptionSelect PASSPHRASE should switch to PassphraseType`() = runTest {
        val viewModel = createViewModel()
        val updatedText = "updatedPassphrase"

        fakeGeneratorRepository.setMockGeneratePasswordResult(
            GeneratedPasswordResult.Success(updatedText),
        )

        val action = GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
            passcodeTypeOption = GeneratorState.MainType.Passcode.PasscodeTypeOption.PASSPHRASE,
        )

        viewModel.actionChannel.trySend(action)

        val expectedState = initialState.copy(
            generatedText = updatedText,
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
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success("defaultPassword"),
            )
            viewModel = GeneratorViewModel(initialSavedStateHandle, fakeGeneratorRepository)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `SliderLengthChange should update password length correctly to new value and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                val newLength = 16

                viewModel.actionChannel.trySend(
                    GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange(
                        length = newLength,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            length = newLength,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleCapitalLettersChange should update useCapitals correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

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
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useCapitals = useCapitals,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleLowercaseLettersChange should update useLowercase correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

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
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useLowercase = useLowercase,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleNumbersChange should update useNumbers correctly and generate text`() = runTest {
            val updatedGeneratedPassword = "updatedPassword"
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success(updatedGeneratedPassword),
            )

            val useNumbers = true

            viewModel.actionChannel.trySend(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange(
                    useNumbers = useNumbers,
                ),
            )

            val expectedState = defaultPasswordState.copy(
                generatedText = updatedGeneratedPassword,
                selectedType = GeneratorState.MainType.Passcode(
                    GeneratorState.MainType.Passcode.PasscodeType.Password(
                        useNumbers = useNumbers,
                    ),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleSpecialCharactersChange should update useSpecialChars correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

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
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            useSpecialChars = useSpecialChars,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `MinNumbersCounterChange should update minNumbers correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

                val minNumbers = 4

                viewModel.actionChannel.trySend(
                    GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange(
                        minNumbers = minNumbers,
                    ),
                )

                val expectedState = defaultPasswordState.copy(
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            minNumbers = minNumbers,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `MinSpecialCharactersChange should update minSpecial correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

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
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            minSpecial = minSpecial,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Suppress("MaxLineLength")
        @Test
        fun `ToggleAvoidAmbigousCharactersChange should update avoidAmbiguousChars correctly and generate text`() =
            runTest {
                val updatedGeneratedPassword = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePasswordResult(
                    GeneratedPasswordResult.Success(updatedGeneratedPassword),
                )

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
                    generatedText = updatedGeneratedPassword,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Password(
                            avoidAmbiguousChars = avoidAmbiguousChars,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
    }

    @Nested
    inner class PassphraseActions {

        private val defaultPassphraseState = createPassphraseState()
        private lateinit var viewModel: GeneratorViewModel

        @BeforeEach
        fun setup() {
            fakeGeneratorRepository.setMockGeneratePasswordResult(
                GeneratedPasswordResult.Success("defaultPassphrase"),
            )
            viewModel = GeneratorViewModel(passphraseSavedStateHandle, fakeGeneratorRepository)
        }

        @Test
        fun `NumWordsCounterChange should update the numWords property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

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
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                            numWords = newNumWords,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `WordSeparatorTextChange should update wordSeparator correctly to new value`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

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
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                            wordSeparator = newWordSeparatorChar,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleIncludeNumberChange should update the includeNumber property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

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
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passcode(
                        selectedType = GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                            includeNumber = true,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }

        @Test
        fun `ToggleCapitalizeChange should update the capitalize property correctly`() =
            runTest {
                val updatedGeneratedPassphrase = "updatedPassword"
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(updatedGeneratedPassphrase),
                )

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
                    generatedText = updatedGeneratedPassphrase,
                    selectedType = GeneratorState.MainType.Passcode(
                        GeneratorState.MainType.Passcode.PasscodeType.Passphrase(
                            capitalize = true,
                        ),
                    ),
                )

                assertEquals(expectedState, viewModel.stateFlow.value)
            }
    }
    //region Helper Functions

    @Suppress("LongParameterList")
    private fun createPasswordState(
        generatedText: String = "defaultPassword",
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
        generatedText: String = "defaultPassphrase",
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

    private fun createUsernameState(): GeneratorState = GeneratorState(
        generatedText = "defaultUsername",
        selectedType = GeneratorState.MainType.Username(),
    )

    private fun createSavedStateHandleWithState(state: GeneratorState) =
        SavedStateHandle().apply {
            set("state", state)
        }

    private fun createViewModel(
        state: GeneratorState? = initialState,
    ): GeneratorViewModel = GeneratorViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        generatorRepository = fakeGeneratorRepository,
    )

    //endregion Helper Functions
}
