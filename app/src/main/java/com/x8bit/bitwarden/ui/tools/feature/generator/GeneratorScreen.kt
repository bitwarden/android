@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenLargeTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithTwoIcons
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Password.Companion.PASSWORD_LENGTH_SLIDER_MAX
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Password.Companion.PASSWORD_LENGTH_SLIDER_MIN

/**
 * Top level composable for the generator screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is GeneratorEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onRegenerateClick: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(GeneratorAction.RegenerateClick) }
    }

    val onCopyClick: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(GeneratorAction.CopyClick) }
    }

    val onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit = remember(viewModel) {
        { viewModel.trySendAction(GeneratorAction.MainTypeOptionSelect(it)) }
    }

    val onPasscodeOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit =
        remember(viewModel) {
            {
                viewModel.trySendAction(
                    GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(
                        it,
                    ),
                )
            }
        }

    val onPasswordSliderLengthChange: (Int) -> Unit = remember(viewModel) {
        { newLength ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.SliderLengthChange(
                    length = newLength,
                ),
            )
        }
    }

    val onPasswordToggleCapitalLettersChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldUseCapitals ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleCapitalLettersChange(
                    useCapitals = shouldUseCapitals,
                ),
            )
        }
    }

    val onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldUseLowercase ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password
                    .ToggleLowercaseLettersChange(
                        useLowercase = shouldUseLowercase,
                    ),
            )
        }
    }

    val onPasswordToggleNumbersChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldUseNumbers ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.ToggleNumbersChange(
                    useNumbers = shouldUseNumbers,
                ),
            )
        }
    }

    val onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldUseSpecialChars ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password
                    .ToggleSpecialCharactersChange(
                        useSpecialChars = shouldUseSpecialChars,
                    ),
            )
        }
    }

    val onPasswordMinNumbersCounterChange: (Int) -> Unit = remember(viewModel) {
        { newMinNumbers ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinNumbersCounterChange(
                    minNumbers = newMinNumbers,
                ),
            )
        }
    }

    val onPasswordMinSpecialCharactersChange: (Int) -> Unit = remember(viewModel) {
        { newMinSpecial ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password.MinSpecialCharactersChange(
                    minSpecial = newMinSpecial,
                ),
            )
        }
    }

    val onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldAvoidAmbiguousChars ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Password
                    .ToggleAvoidAmbigousCharactersChange(
                        avoidAmbiguousChars = shouldAvoidAmbiguousChars,
                    ),
            )
        }
    }

    val onPassphraseNumWordsCounterChange: (Int) -> Unit = remember(viewModel) {
        { changeInCounter ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange(
                    numWords = changeInCounter,
                ),
            )
        }
    }

    val onPassphraseCapitalizeToggleChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldCapitalize ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange(
                    capitalize = shouldCapitalize,
                ),
            )
        }
    }

    val onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldIncludeNumber ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange(
                    includeNumber = shouldIncludeNumber,
                ),
            )
        }
    }

    val onPassphraseWordSeparatorChange: (Char?) -> Unit = remember(viewModel) {
        { newSeparator ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange(
                    wordSeparator = newSeparator,
                ),
            )
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            BitwardenLargeTopAppBar(
                title = stringResource(id = R.string.generator),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        ScrollContent(
            state = state,
            onRegenerateClick = onRegenerateClick,
            onCopyClick = onCopyClick,
            onMainStateOptionClicked = onMainStateOptionClicked,
            onSubStateOptionClicked = onPasscodeOptionClicked,

            // Password handlers
            onPasswordSliderLengthChange = onPasswordSliderLengthChange,
            onPasswordToggleCapitalLettersChange = onPasswordToggleCapitalLettersChange,
            onPasswordToggleLowercaseLettersChange = onPasswordToggleLowercaseLettersChange,
            onPasswordToggleNumbersChange = onPasswordToggleNumbersChange,
            onPasswordToggleSpecialCharactersChange = onPasswordToggleSpecialCharactersChange,
            onPasswordMinNumbersCounterChange = onPasswordMinNumbersCounterChange,
            onPasswordMinSpecialCharactersChange = onPasswordMinSpecialCharactersChange,
            onPasswordToggleAvoidAmbiguousCharsChange = onPasswordToggleAvoidAmbiguousCharsChange,

            // Passphrase handlers
            onPassphraseNumWordsCounterChange = onPassphraseNumWordsCounterChange,
            onPassphraseWordSeparatorChange = onPassphraseWordSeparatorChange,
            onPassphraseCapitalizeToggleChange = onPassphraseCapitalizeToggleChange,
            onPassphraseIncludeNumberToggleChange = onPassphraseIncludeNumberToggleChange,

            modifier = Modifier.padding(innerPadding),
        )
    }
}

//region ScrollContent and Static Items

@Suppress("LongMethod")
@Composable
private fun ScrollContent(
    state: GeneratorState,
    onRegenerateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    onPasswordSliderLengthChange: (Int) -> Unit,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
    onPassphraseWordSeparatorChange: (Char?) -> Unit,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {

        GeneratedStringItem(
            generatedText = state.generatedText,
            onCopyClick = onCopyClick,
            onRegenerateClick = onRegenerateClick,
        )

        MainStateOptionsItem(
            selectedType = state.selectedType,
            possibleMainStates = state.typeOptions,
            onMainStateOptionClicked = onMainStateOptionClicked,
        )

        Row(
            Modifier.height(32.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(id = R.string.options),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passcode -> {
                PasscodeTypeItems(
                    passcodeState = selectedType,
                    onSubStateOptionClicked = onSubStateOptionClicked,

                    // Password handlers
                    onPasswordSliderLengthChange = onPasswordSliderLengthChange,
                    onPasswordToggleCapitalLettersChange = onPasswordToggleCapitalLettersChange,
                    onPasswordToggleLowercaseLettersChange = onPasswordToggleLowercaseLettersChange,
                    onPasswordToggleNumbersChange = onPasswordToggleNumbersChange,
                    onPasswordToggleSpecialCharactersChange =
                    onPasswordToggleSpecialCharactersChange,
                    onPasswordMinNumbersCounterChange = onPasswordMinNumbersCounterChange,
                    onPasswordMinSpecialCharactersChange = onPasswordMinSpecialCharactersChange,
                    onPasswordToggleAvoidAmbiguousCharsChange =
                    onPasswordToggleAvoidAmbiguousCharsChange,

                    // Passphrase handlers
                    onPassphraseNumWordsCounterChange = onPassphraseNumWordsCounterChange,
                    onPassphraseWordSeparatorChange = onPassphraseWordSeparatorChange,
                    onPassphraseCapitalizeToggleChange = onPassphraseCapitalizeToggleChange,
                    onPassphraseIncludeNumberToggleChange = onPassphraseIncludeNumberToggleChange,
                )
            }

            is GeneratorState.MainType.Username -> {
                // TODO(BIT-335): Username state to handle Plus Addressed Email
            }
        }
    }
}

@Composable
private fun GeneratedStringItem(
    generatedText: String,
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit,
) {
    BitwardenTextFieldWithTwoIcons(
        label = "",
        value = generatedText,
        firstIconResource = IconResource(
            iconPainter = painterResource(R.drawable.ic_copy),
            contentDescription = stringResource(id = R.string.copy),
        ),
        onFirstIconClick = onCopyClick,
        secondIconResource = IconResource(
            iconPainter = painterResource(R.drawable.ic_generator),
            contentDescription = stringResource(id = R.string.generate_password),
        ),
        onSecondIconClick = onRegenerateClick,
    )
}

@Composable
private fun MainStateOptionsItem(
    selectedType: GeneratorState.MainType,
    possibleMainStates: List<GeneratorState.MainTypeOption>,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
) {
    val optionsWithStrings =
        possibleMainStates.associateBy({ it }, { stringResource(id = it.labelRes) })

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.what_would_you_like_to_generate),
        options = optionsWithStrings.values.toList(),
        selectedOption = stringResource(id = selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onMainStateOptionClicked(selectedOptionId)
        },
    )
}

//endregion ScrollContent and Static Items

//region PasscodeType Composables

@Composable
private fun PasscodeTypeItems(
    passcodeState: GeneratorState.MainType.Passcode,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    onPasswordSliderLengthChange: (Int) -> Unit,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
    onPassphraseWordSeparatorChange: (Char?) -> Unit,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    PasscodeOptionsItem(passcodeState, onSubStateOptionClicked)

    when (val selectedType = passcodeState.selectedType) {
        is GeneratorState.MainType.Passcode.PasscodeType.Password -> {
            PasswordTypeContent(
                passwordTypeState = selectedType,
                onPasswordSliderLengthChange = onPasswordSliderLengthChange,
                onPasswordToggleCapitalLettersChange = onPasswordToggleCapitalLettersChange,
                onPasswordToggleLowercaseLettersChange = onPasswordToggleLowercaseLettersChange,
                onPasswordToggleNumbersChange = onPasswordToggleNumbersChange,
                onPasswordToggleSpecialCharactersChange = onPasswordToggleSpecialCharactersChange,
                onPasswordMinNumbersCounterChange = onPasswordMinNumbersCounterChange,
                onPasswordMinSpecialCharactersChange = onPasswordMinSpecialCharactersChange,
                onPasswordToggleAvoidAmbiguousCharsChange =
                onPasswordToggleAvoidAmbiguousCharsChange,
            )
        }

        is GeneratorState.MainType.Passcode.PasscodeType.Passphrase -> {
            PassphraseTypeContent(
                passphraseTypeState = selectedType,
                onPassphraseNumWordsCounterChange = onPassphraseNumWordsCounterChange,
                onPassphraseWordSeparatorChange = onPassphraseWordSeparatorChange,
                onPassphraseCapitalizeToggleChange = onPassphraseCapitalizeToggleChange,
                onPassphraseIncludeNumberToggleChange = onPassphraseIncludeNumberToggleChange,
            )
        }
    }
}

@Composable
private fun PasscodeOptionsItem(
    currentSubState: GeneratorState.MainType.Passcode,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
) {
    val possibleSubStates = GeneratorState.MainType.Passcode.PasscodeTypeOption.values().toList()
    val optionsWithStrings =
        possibleSubStates.associateBy({ it }, { stringResource(id = it.labelRes) })

    BitwardenMultiSelectButton(
        label = stringResource(id = currentSubState.selectedType.displayStringResId),
        options = optionsWithStrings.values.toList(),
        selectedOption = stringResource(id = currentSubState.selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
    )
}

//endregion PasscodeType Composables

//region PasswordType Composables

@Composable
private fun PasswordTypeContent(
    passwordTypeState: GeneratorState.MainType.Passcode.PasscodeType.Password,
    onPasswordSliderLengthChange: (Int) -> Unit,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
) {
    PasswordLengthSliderItem(
        length = passwordTypeState.length,
        onPasswordSliderLengthChange = onPasswordSliderLengthChange,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {

        PasswordCapitalLettersToggleItem(
            useCapitals = passwordTypeState.useCapitals,
            onPasswordToggleCapitalLettersChange = onPasswordToggleCapitalLettersChange,
        )
        PasswordLowercaseLettersToggleItem(
            useLowercase = passwordTypeState.useLowercase,
            onPasswordToggleLowercaseLettersChange = onPasswordToggleLowercaseLettersChange,
        )
        PasswordNumbersToggleItem(
            useNumbers = passwordTypeState.useNumbers,
            onPasswordToggleNumbersChange = onPasswordToggleNumbersChange,
        )
        PasswordSpecialCharactersToggleItem(
            useSpecialChars = passwordTypeState.useSpecialChars,
            onPasswordToggleSpecialCharactersChange = onPasswordToggleSpecialCharactersChange,
        )
    }
    PasswordMinNumbersCounterItem(
        minNumbers = passwordTypeState.minNumbers,
        onPasswordMinNumbersCounterChange = onPasswordMinNumbersCounterChange,
    )
    PasswordMinSpecialCharactersCounterItem(
        minSpecial = passwordTypeState.minSpecial,
        onPasswordMinSpecialCharactersChange = onPasswordMinSpecialCharactersChange,
    )
    PasswordAvoidAmbiguousCharsToggleItem(
        avoidAmbiguousChars = passwordTypeState.avoidAmbiguousChars,
        onPasswordToggleAvoidAmbiguousCharsChange = onPasswordToggleAvoidAmbiguousCharsChange,
    )
}

@Composable
private fun PasswordLengthSliderItem(
    length: Int,
    onPasswordSliderLengthChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        OutlinedTextField(
            value = length.toString(),
            readOnly = true,
            onValueChange = { newText ->
                newText.toIntOrNull()?.let { newValue ->
                    onPasswordSliderLengthChange(newValue)
                }
            },
            label = { Text(stringResource(id = R.string.length)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 71.dp),
        )

        Slider(
            value = length.toFloat(),
            onValueChange = { newValue ->
                onPasswordSliderLengthChange(newValue.toInt())
            },
            valueRange =
            PASSWORD_LENGTH_SLIDER_MIN.toFloat()..PASSWORD_LENGTH_SLIDER_MAX.toFloat(),
            steps = PASSWORD_LENGTH_SLIDER_MAX - 1,
        )
    }
}

@Composable
private fun PasswordCapitalLettersToggleItem(
    useCapitals: Boolean,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.uppercase_ato_z),
        isChecked = useCapitals,
        onCheckedChange = onPasswordToggleCapitalLettersChange,
    )
}

@Composable
private fun PasswordLowercaseLettersToggleItem(
    useLowercase: Boolean,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.lowercase_ato_z),
        isChecked = useLowercase,
        onCheckedChange = onPasswordToggleLowercaseLettersChange,
    )
}

@Composable
private fun PasswordNumbersToggleItem(
    useNumbers: Boolean,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.numbers_zero_to_nine),
        isChecked = useNumbers,
        onCheckedChange = onPasswordToggleNumbersChange,
    )
}

@Composable
private fun PasswordSpecialCharactersToggleItem(
    useSpecialChars: Boolean,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.special_characters),
        isChecked = useSpecialChars,
        onCheckedChange = onPasswordToggleSpecialCharactersChange,
    )
}

@Composable
private fun PasswordMinNumbersCounterItem(
    minNumbers: Int,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
) {
    BitwardenTextFieldWithTwoIcons(
        label = stringResource(id = R.string.min_numbers),
        value = minNumbers.toString(),
        firstIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_minus),
            contentDescription = "\u2212",
        ),
        onFirstIconClick = {
            onPasswordMinNumbersCounterChange(minNumbers - 1)
        },
        secondIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "+",
        ),
        onSecondIconClick = {
            onPasswordMinNumbersCounterChange(minNumbers + 1)
        },
    )
}

@Composable
private fun PasswordMinSpecialCharactersCounterItem(
    minSpecial: Int,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
) {
    BitwardenTextFieldWithTwoIcons(
        label = stringResource(id = R.string.min_special),
        value = minSpecial.toString(),
        firstIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_minus),
            contentDescription = "\u2212",
        ),
        onFirstIconClick = {
            onPasswordMinSpecialCharactersChange(minSpecial - 1)
        },
        secondIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "+",
        ),
        onSecondIconClick = {
            onPasswordMinSpecialCharactersChange(minSpecial + 1)
        },
    )
}

@Composable
private fun PasswordAvoidAmbiguousCharsToggleItem(
    avoidAmbiguousChars: Boolean,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.avoid_ambiguous_characters),
        isChecked = avoidAmbiguousChars,
        onCheckedChange = onPasswordToggleAvoidAmbiguousCharsChange,
    )
}

//endregion PasswordType Composables

//region PassphraseType Composables

@Composable
private fun PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passcode.PasscodeType.Passphrase,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
    onPassphraseWordSeparatorChange: (Char?) -> Unit,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    PassphraseNumWordsCounterItem(
        numWords = passphraseTypeState.numWords,
        onPassphraseNumWordsCounterChange = onPassphraseNumWordsCounterChange,
    )
    PassphraseWordSeparatorInputItem(
        wordSeparator = passphraseTypeState.wordSeparator,
        onPassphraseWordSeparatorChange = onPassphraseWordSeparatorChange,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        PassphraseCapitalizeToggleItem(
            capitalize = passphraseTypeState.capitalize,
            onPassphraseCapitalizeToggleChange = onPassphraseCapitalizeToggleChange,
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onPassphraseIncludeNumberToggleChange = onPassphraseIncludeNumberToggleChange,
        )
    }
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
) {
    BitwardenTextFieldWithTwoIcons(
        label = stringResource(id = R.string.number_of_words),
        value = numWords.toString(),
        firstIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_minus),
            contentDescription = "\u2212",
        ),
        onFirstIconClick = {
            onPassphraseNumWordsCounterChange(numWords - 1)
        },
        secondIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "+",
        ),
        onSecondIconClick = {
            onPassphraseNumWordsCounterChange(numWords + 1)
        },
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onPassphraseWordSeparatorChange: (wordSeparator: Char?) -> Unit,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.word_separator),
        value = wordSeparator?.toString() ?: "",
        onValueChange = {
            onPassphraseWordSeparatorChange(it.toCharArray().firstOrNull())
        },
        modifier = Modifier.width(267.dp),
    )
}

@Composable
private fun PassphraseCapitalizeToggleItem(
    capitalize: Boolean,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.capitalize),
        isChecked = capitalize,
        onCheckedChange = onPassphraseCapitalizeToggleChange,
    )
}

@Composable
private fun PassphraseIncludeNumberToggleItem(
    includeNumber: Boolean,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        onCheckedChange = onPassphraseIncludeNumberToggleChange,
    )
}

//endregion PassphraseType Composables

@Preview(showBackground = true)
@Composable
private fun GeneratorPreview() {
    BitwardenTheme {
        GeneratorScreen()
    }
}
