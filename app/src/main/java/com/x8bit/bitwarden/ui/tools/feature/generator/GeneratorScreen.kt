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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

    val onNumWordsCounterChange: (Int) -> Unit = remember(viewModel) {
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

    val onIncludeNumberToggleChange: (Boolean) -> Unit = remember(viewModel) {
        { shouldIncludeNumber ->
            viewModel.trySendAction(
                GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange(
                    includeNumber = shouldIncludeNumber,
                ),
            )
        }
    }

    val onWordSeparatorChange: (Char?) -> Unit = remember(viewModel) {
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
            state,
            onRegenerateClick,
            onCopyClick,
            onMainStateOptionClicked,
            onPasscodeOptionClicked,
            onNumWordsCounterChange,
            onWordSeparatorChange,
            onPassphraseCapitalizeToggleChange,
            onIncludeNumberToggleChange,
            Modifier.padding(innerPadding),
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
    onNumWordsCounterChange: (Int) -> Unit,
    onWordSeparatorChange: (Char?) -> Unit,
    onCapitalizeToggleChange: (Boolean) -> Unit,
    onIncludeNumberToggleChange: (Boolean) -> Unit,
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
                    selectedType,
                    onSubStateOptionClicked,
                    onNumWordsCounterChange,
                    onWordSeparatorChange,
                    onCapitalizeToggleChange,
                    onIncludeNumberToggleChange,
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

/**
 * A composable function to represent a collection of passcode type items based on the selected
 * [GeneratorState.MainType.Passcode.PasscodeType]. It dynamically displays content depending on
 * the currently selected passcode type.
 *
 * @param passcodeState The current state of the passcode generator,
 * holding the selected passcode type and other settings.
 * @param onSubStateOptionClicked A lambda function invoked when a substate option is clicked.
 * It takes the selected [GeneratorState.MainType.Passcode.PasscodeTypeOption] as a parameter.
 * @param onNumWordsCounterChange A lambda function invoked when there is a change
 * in the number of words for passphrase. It takes the updated number of words as a parameter.
 * @param onWordSeparatorChange A lambda function invoked when there is a change
 * in the word separator character for passphrase. It takes the updated character as a parameter,
 * `null` if there is no separator.
 * @param onCapitalizeToggleChange A lambda function invoked when the capitalize
 * toggle state changes for passphrase. It takes the updated toggle state as a parameter.
 * @param onIncludeNumberToggleChange A lambda function invoked when the include number toggle
 * state changes for passphrase. It takes the updated toggle state as a parameter.
 */
@Composable
fun PasscodeTypeItems(
    passcodeState: GeneratorState.MainType.Passcode,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    onNumWordsCounterChange: (Int) -> Unit,
    onWordSeparatorChange: (Char?) -> Unit,
    onCapitalizeToggleChange: (Boolean) -> Unit,
    onIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    PasscodeOptionsItem(passcodeState, onSubStateOptionClicked)

    when (val selectedType = passcodeState.selectedType) {
        is GeneratorState.MainType.Passcode.PasscodeType.Passphrase -> {
            PassphraseTypeContent(
                selectedType,
                onNumWordsCounterChange,
                onWordSeparatorChange,
                onCapitalizeToggleChange,
                onIncludeNumberToggleChange,
            )
        }

        is GeneratorState.MainType.Passcode.PasscodeType.Password -> {
            // TODO(BIT-334): Render UI for Password type
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

//region PassphraseType Composables

@Composable
private fun PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passcode.PasscodeType.Passphrase,
    onNumWordsCounterChange: (Int) -> Unit,
    onWordSeparatorChange: (Char?) -> Unit,
    onCapitalizeToggleChange: (Boolean) -> Unit,
    onIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    PassphraseNumWordsCounterItem(
        numWords = passphraseTypeState.numWords,
        onNumWordsCounterChange = onNumWordsCounterChange,
    )
    PassphraseWordSeparatorInputItem(
        wordSeparator = passphraseTypeState.wordSeparator,
        onWordSeparatorChange = onWordSeparatorChange,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        PassphraseCapitalizeToggleItem(
            capitalize = passphraseTypeState.capitalize,
            onPassphraseCapitalizeToggleChange = onCapitalizeToggleChange,
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onIncludeNumberToggleChange = onIncludeNumberToggleChange,
        )
    }
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onNumWordsCounterChange: (Int) -> Unit,
) {
    BitwardenTextFieldWithTwoIcons(
        label = stringResource(id = R.string.number_of_words),
        value = numWords.toString(),
        firstIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_minus),
            contentDescription = "\u2212",
        ),
        onFirstIconClick = {
            onNumWordsCounterChange(numWords - 1)
        },
        secondIconResource = IconResource(
            iconPainter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "+",
        ),
        onSecondIconClick = {
            onNumWordsCounterChange(numWords + 1)
        },
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onWordSeparatorChange: (wordSeparator: Char?) -> Unit,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.word_separator),
        value = wordSeparator?.toString() ?: "",
        onValueChange = {
            onWordSeparatorChange(it.toCharArray().firstOrNull())
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
    onIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        onCheckedChange = onIncludeNumberToggleChange,
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
