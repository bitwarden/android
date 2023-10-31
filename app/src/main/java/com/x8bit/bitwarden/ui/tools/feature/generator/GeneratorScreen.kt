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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.toDp
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenReadOnlyTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
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

    val passwordHandlers = PasswordHandlers.create(viewModel = viewModel)

    val passphraseHandlers = PassphraseHandlers.create(viewModel = viewModel)

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.generator),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenOverflowActionItem()
                },
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
            passwordHandlers = passwordHandlers,
            passphraseHandlers = passphraseHandlers,
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
    passwordHandlers: PasswordHandlers,
    passphraseHandlers: PassphraseHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.surface)
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
            BitwardenListHeaderText(
                label = stringResource(id = R.string.options),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passcode -> {
                PasscodeTypeItems(
                    passcodeState = selectedType,
                    onSubStateOptionClicked = onSubStateOptionClicked,
                    passwordHandlers = passwordHandlers,
                    passphraseHandlers = passphraseHandlers,
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
    BitwardenReadOnlyTextFieldWithActions(
        label = "",
        value = generatedText,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = stringResource(id = R.string.copy),
                ),
                onClick = onCopyClick,
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_generator),
                    contentDescription = stringResource(id = R.string.generate_password),
                ),
                onClick = onRegenerateClick,
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
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
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    )
}

//endregion ScrollContent and Static Items

//region PasscodeType Composables

@Composable
private fun PasscodeTypeItems(
    passcodeState: GeneratorState.MainType.Passcode,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    passwordHandlers: PasswordHandlers,
    passphraseHandlers: PassphraseHandlers,
) {
    PasscodeOptionsItem(passcodeState, onSubStateOptionClicked)

    when (val selectedType = passcodeState.selectedType) {
        is GeneratorState.MainType.Passcode.PasscodeType.Password -> {
            PasswordTypeContent(
                passwordTypeState = selectedType,
                passwordHandlers = passwordHandlers,
            )
        }

        is GeneratorState.MainType.Passcode.PasscodeType.Passphrase -> {
            PassphraseTypeContent(
                passphraseTypeState = selectedType,
                passphraseHandlers = passphraseHandlers,
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
        label = stringResource(id = R.string.password_type),
        options = optionsWithStrings.values.toList(),
        selectedOption = stringResource(id = currentSubState.selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    )
}

//endregion PasscodeType Composables

//region PasswordType Composables

@Composable
private fun PasswordTypeContent(
    passwordTypeState: GeneratorState.MainType.Passcode.PasscodeType.Password,
    passwordHandlers: PasswordHandlers,
) {
    PasswordLengthSliderItem(
        length = passwordTypeState.length,
        onPasswordSliderLengthChange =
        passwordHandlers.onPasswordSliderLengthChange,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {

        PasswordCapitalLettersToggleItem(
            useCapitals = passwordTypeState.useCapitals,
            onPasswordToggleCapitalLettersChange =
            passwordHandlers.onPasswordToggleCapitalLettersChange,
        )
        PasswordLowercaseLettersToggleItem(
            useLowercase = passwordTypeState.useLowercase,
            onPasswordToggleLowercaseLettersChange =
            passwordHandlers.onPasswordToggleLowercaseLettersChange,
        )
        PasswordNumbersToggleItem(
            useNumbers = passwordTypeState.useNumbers,
            onPasswordToggleNumbersChange =
            passwordHandlers.onPasswordToggleNumbersChange,
        )
        PasswordSpecialCharactersToggleItem(
            useSpecialChars = passwordTypeState.useSpecialChars,
            onPasswordToggleSpecialCharactersChange =
            passwordHandlers.onPasswordToggleSpecialCharactersChange,
        )
    }
    PasswordMinNumbersCounterItem(
        minNumbers = passwordTypeState.minNumbers,
        onPasswordMinNumbersCounterChange =
        passwordHandlers.onPasswordMinNumbersCounterChange,
    )
    PasswordMinSpecialCharactersCounterItem(
        minSpecial = passwordTypeState.minSpecial,
        onPasswordMinSpecialCharactersChange =
        passwordHandlers.onPasswordMinSpecialCharactersChange,
    )
    PasswordAvoidAmbiguousCharsToggleItem(
        avoidAmbiguousChars = passwordTypeState.avoidAmbiguousChars,
        onPasswordToggleAvoidAmbiguousCharsChange =
        passwordHandlers.onPasswordToggleAvoidAmbiguousCharsChange,
    )
}

@Composable
private fun PasswordLengthSliderItem(
    length: Int,
    onPasswordSliderLengthChange: (Int) -> Unit,
) {
    var labelTextWidth by remember { mutableStateOf(Dp.Unspecified) }

    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
            label = {
                Text(
                    text = stringResource(id = R.string.length),
                    modifier = Modifier
                        .onGloballyPositioned {
                            labelTextWidth = it.size.width.toDp(density)
                        },
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .wrapContentWidth()
                // We want the width to be no wider than the label + 16dp on either side
                .width(labelTextWidth + 16.dp + 16.dp),
        )

        Slider(
            value = length.toFloat(),
            onValueChange = { newValue ->
                onPasswordSliderLengthChange(newValue.toInt())
            },
            valueRange =
            PASSWORD_LENGTH_SLIDER_MIN.toFloat()..PASSWORD_LENGTH_SLIDER_MAX.toFloat(),
            steps = PASSWORD_LENGTH_SLIDER_MAX - 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PasswordCapitalLettersToggleItem(
    useCapitals: Boolean,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = "A—Z",
        isChecked = useCapitals,
        onCheckedChange = onPasswordToggleCapitalLettersChange,
        modifier = Modifier.padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.uppercase_ato_z),
    )
}

@Composable
private fun PasswordLowercaseLettersToggleItem(
    useLowercase: Boolean,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = "a—z",
        isChecked = useLowercase,
        onCheckedChange = onPasswordToggleLowercaseLettersChange,
        modifier = Modifier.padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.lowercase_ato_z),
    )
}

@Composable
private fun PasswordNumbersToggleItem(
    useNumbers: Boolean,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = "0-9",
        isChecked = useNumbers,
        onCheckedChange = onPasswordToggleNumbersChange,
        modifier = Modifier.padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.numbers_zero_to_nine),
    )
}

@Composable
private fun PasswordSpecialCharactersToggleItem(
    useSpecialChars: Boolean,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = "!@#$%^&*",
        isChecked = useSpecialChars,
        onCheckedChange = onPasswordToggleSpecialCharactersChange,
        modifier = Modifier.padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.special_characters),
    )
}

@Composable
private fun PasswordMinNumbersCounterItem(
    minNumbers: Int,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
) {
    BitwardenReadOnlyTextFieldWithActions(
        label = stringResource(id = R.string.min_numbers),
        value = minNumbers.toString(),
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "\u2212",
                ),
                onClick = {
                    onPasswordMinNumbersCounterChange(minNumbers - 1)
                },
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "+",
                ),
                onClick = {
                    onPasswordMinNumbersCounterChange(minNumbers + 1)
                },
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordMinSpecialCharactersCounterItem(
    minSpecial: Int,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
) {
    BitwardenReadOnlyTextFieldWithActions(
        label = stringResource(id = R.string.min_special),
        value = minSpecial.toString(),
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "\u2212",
                ),
                onClick = {
                    onPasswordMinSpecialCharactersChange(minSpecial - 1)
                },
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "+",
                ),
                onClick = {
                    onPasswordMinSpecialCharactersChange(minSpecial + 1)
                },
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

//endregion PasswordType Composables

//region PassphraseType Composables

@Composable
private fun PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passcode.PasscodeType.Passphrase,
    passphraseHandlers: PassphraseHandlers,
) {
    PassphraseNumWordsCounterItem(
        numWords = passphraseTypeState.numWords,
        onPassphraseNumWordsCounterChange =
        passphraseHandlers.onPassphraseNumWordsCounterChange,
    )
    PassphraseWordSeparatorInputItem(
        wordSeparator = passphraseTypeState.wordSeparator,
        onPassphraseWordSeparatorChange =
        passphraseHandlers.onPassphraseWordSeparatorChange,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        PassphraseCapitalizeToggleItem(
            capitalize = passphraseTypeState.capitalize,
            onPassphraseCapitalizeToggleChange =
            passphraseHandlers.onPassphraseCapitalizeToggleChange,
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onPassphraseIncludeNumberToggleChange =
            passphraseHandlers.onPassphraseIncludeNumberToggleChange,
        )
    }
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
) {
    BitwardenReadOnlyTextFieldWithActions(
        label = stringResource(id = R.string.number_of_words),
        value = numWords.toString(),
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "\u2212",
                ),
                onClick = {
                    onPassphraseNumWordsCounterChange(numWords - 1)
                },
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "+",
                ),
                onClick = {
                    onPassphraseNumWordsCounterChange(numWords + 1)
                },
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
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
        modifier = Modifier
            .width(267.dp)
            .padding(horizontal = 16.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp),
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

/**
 * A class dedicated to handling user interactions related to password configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
private class PasswordHandlers(
    val onPasswordSliderLengthChange: (Int) -> Unit,
    val onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    val onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    val onPasswordToggleNumbersChange: (Boolean) -> Unit,
    val onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    val onPasswordMinNumbersCounterChange: (Int) -> Unit,
    val onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    val onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
) {
    companion object {
        @Suppress("LongMethod")
        fun create(viewModel: GeneratorViewModel): PasswordHandlers {
            return PasswordHandlers(
                onPasswordSliderLengthChange = { newLength ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .SliderLengthChange(
                                length = newLength,
                            ),
                    )
                },
                onPasswordToggleCapitalLettersChange = { shouldUseCapitals ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .ToggleCapitalLettersChange(
                                useCapitals = shouldUseCapitals,
                            ),
                    )
                },
                onPasswordToggleLowercaseLettersChange = { shouldUseLowercase ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .ToggleLowercaseLettersChange(
                                useLowercase = shouldUseLowercase,
                            ),
                    )
                },
                onPasswordToggleNumbersChange = { shouldUseNumbers ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .ToggleNumbersChange(
                                useNumbers = shouldUseNumbers,
                            ),
                    )
                },
                onPasswordToggleSpecialCharactersChange = { shouldUseSpecialChars ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .ToggleSpecialCharactersChange(
                                useSpecialChars = shouldUseSpecialChars,
                            ),
                    )
                },
                onPasswordMinNumbersCounterChange = { newMinNumbers ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .MinNumbersCounterChange(
                                minNumbers = newMinNumbers,
                            ),
                    )
                },
                onPasswordMinSpecialCharactersChange = { newMinSpecial ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .MinSpecialCharactersChange(
                                minSpecial = newMinSpecial,
                            ),
                    )
                },
                onPasswordToggleAvoidAmbiguousCharsChange = { shouldAvoidAmbiguousChars ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .ToggleAvoidAmbigousCharactersChange(
                                avoidAmbiguousChars = shouldAvoidAmbiguousChars,
                            ),
                    )
                },
            )
        }
    }
}

/**
 * A class dedicated to handling user interactions related to passphrase configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
private class PassphraseHandlers(
    val onPassphraseNumWordsCounterChange: (Int) -> Unit,
    val onPassphraseWordSeparatorChange: (Char?) -> Unit,
    val onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    val onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    companion object {
        fun create(viewModel: GeneratorViewModel): PassphraseHandlers {
            return PassphraseHandlers(
                onPassphraseNumWordsCounterChange = { changeInCounter ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Passphrase
                            .NumWordsCounterChange(
                                numWords = changeInCounter,
                            ),
                    )
                },
                onPassphraseWordSeparatorChange = { newSeparator ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Passphrase
                            .WordSeparatorTextChange(
                                wordSeparator = newSeparator,
                            ),
                    )
                },
                onPassphraseCapitalizeToggleChange = { shouldCapitalize ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Passphrase
                            .ToggleCapitalizeChange(
                                capitalize = shouldCapitalize,
                            ),
                    )
                },
                onPassphraseIncludeNumberToggleChange = { shouldIncludeNumber ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Passphrase
                            .ToggleIncludeNumberChange(
                                includeNumber = shouldIncludeNumber,
                            ),
                    )
                },
            )
        }
    }
}
