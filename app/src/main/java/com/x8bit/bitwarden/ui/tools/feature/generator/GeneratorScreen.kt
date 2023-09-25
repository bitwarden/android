@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the generator screen.
 */
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit = {
        viewModel.trySendAction(GeneratorAction.MainTypeOptionSelect(it))
    }

    val onPasscodeOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit = {
        viewModel.trySendAction(GeneratorAction.MainType.Passcode.PasscodeTypeOptionSelect(it))
    }

    val onNumWordsCounterChange: (Int) -> Unit = { changeInCounter ->
        viewModel.trySendAction(
            GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.NumWordsCounterChange(
                numWords = changeInCounter,
            ),
        )
    }

    val onPassphraseCapitalizeToggleChange: (Boolean) -> Unit = { shouldCapitalize ->
        viewModel.trySendAction(
            GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleCapitalizeChange(
                capitalize = shouldCapitalize,
            ),
        )
    }

    val onIncludeNumberToggleChange: (Boolean) -> Unit = { shouldIncludeNumber ->
        viewModel.trySendAction(
            GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.ToggleIncludeNumberChange(
                includeNumber = shouldIncludeNumber,
            ),
        )
    }

    val onWordSeparatorChange: (Char?) -> Unit = { newSeparator ->
        viewModel.trySendAction(
            GeneratorAction.MainType.Passcode.PasscodeType.Passphrase.WordSeparatorTextChange(
                wordSeparator = newSeparator,
            ),
        )
    }

    Scaffold(
        topBar = { TopAppBar() },
    ) { innerPadding ->
        ScrollContent(
            state,
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

//region TopAppBar Composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar() {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        title = {
            Text(
                stringResource(id = R.string.generator),
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        navigationIcon = {
            Spacer(Modifier.width(40.dp))
        },
        actions = {
            OverflowMenu()
        },
    )
}

@Composable
private fun OverflowMenu() {
    IconButton(
        onClick = {},
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(id = R.string.options),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

//endregion TopAppBar Composables

//region ScrollContent and Static Items

@Composable
private fun ScrollContent(
    state: GeneratorState,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
    onSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    onNumWordsCounterChange: (Int) -> Unit,
    onWordSeparatorChange: (Char?) -> Unit,
    onCapitalizeToggleChange: (Boolean) -> Unit,
    onIncludeNumberToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GeneratedStringItem(state.generatedText)
        MainStateOptionsItem(
            selectedType = state.selectedType,
            possibleMainStates = state.typeOptions,
            onMainStateOptionClicked = onMainStateOptionClicked,
        )

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
private fun GeneratedStringItem(generatedText: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = generatedText)

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.copy),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.generate_password),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainStateOptionsItem(
    selectedType: GeneratorState.MainType,
    possibleMainStates: List<GeneratorState.MainTypeOption>,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
) {
    val optionsWithStrings =
        possibleMainStates.associateBy({ it }, { stringResource(id = it.labelRes) })

    OptionsSelectionItem(
        title = stringResource(id = R.string.what_would_you_like_to_generate),
        showOptionsText = false,
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

    OptionsSelectionItem(
        title = stringResource(id = currentSubState.selectedType.displayStringResId),
        showOptionsText = true,
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
    PassphraseCapitalizeToggleItem(
        capitalize = passphraseTypeState.capitalize,
        onPassphraseCapitalizeToggleChange = onCapitalizeToggleChange,
    )
    PassphraseIncludeNumberToggleItem(
        includeNumber = passphraseTypeState.includeNumber,
        onIncludeNumberToggleChange = onIncludeNumberToggleChange,
    )
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onNumWordsCounterChange: (Int) -> Unit,
) {
    CounterItem(
        label = stringResource(id = R.string.number_of_words),
        counter = numWords,
        counterValueChange = onNumWordsCounterChange,
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onWordSeparatorChange: (wordSeparator: Char?) -> Unit,
) {
    TextInputItem(
        title = stringResource(id = R.string.word_separator),
        defaultText = wordSeparator?.toString() ?: "",
        textInputChange = {
            onWordSeparatorChange(it.toCharArray().firstOrNull())
        },
    )
}

@Composable
private fun PassphraseCapitalizeToggleItem(
    capitalize: Boolean,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
) {
    SwitchItem(
        title = stringResource(id = R.string.capitalize),
        isToggled = capitalize,
        onToggleChange = onPassphraseCapitalizeToggleChange,
    )
}

@Composable
private fun PassphraseIncludeNumberToggleItem(
    includeNumber: Boolean,
    onIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    SwitchItem(
        title = stringResource(id = R.string.include_number),
        isToggled = includeNumber,
        onToggleChange = onIncludeNumberToggleChange,
    )
}

//endregion PassphraseType Composables

//region Generic Control Composables

/**
 * This composable function renders an item for selecting options, with a capability
 * to expand and collapse the options. It also optionally displays a text indicating
 * that there are multiple options to choose from.
 *
 * @param title The title of the item. This string will be displayed above the selected option.
 * @param showOptionsText A boolean flag that determines whether to show the Options header text.
 * @param options A list of strings representing the available options for selection.
 * @param selectedOption The currently selected option. This will be displayed on the item.
 * @param onOptionSelected A callback invoked when an option is selected, passing the selected
 */
@Composable
private fun OptionsSelectionItem(
    title: String,
    showOptionsText: Boolean = false,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    CommonPadding {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (showOptionsText) {
                Text(
                    stringResource(id = R.string.options),
                    style = TextStyle(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = TextStyle(fontSize = 10.sp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }) {
                Text(selectedOption)

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    options.forEach { optionString ->
                        DropdownMenuItem(
                            text = { Text(text = optionString) },
                            onClick = {
                                expanded = false
                                onOptionSelected(optionString)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable function to represent a counter item, which consists of a label,
 * decrement button, an increment button, and display of the current counter.
 *
 * @param label The text to be displayed as a label for the counter item.
 * @param counter The current value of the counter.
 * @param counterValueChange A lambda function invoked when there is a change in the counter value.
 * It takes the updated counter value as a parameter.
 */
@Composable
private fun CounterItem(
    label: String,
    counter: Int,
    counterValueChange: (Int) -> Unit,
) {
    CommonPadding {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { counterValueChange(counter - 1) },
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(counter.toString())

                IconButton(
                    onClick = { counterValueChange(counter + 1) },
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * A composable function to represent a text input item, which consists of a title,
 * an optional context text above the title, and a text field for input.
 *
 * @param title The title of the text input item.
 * @param defaultText The default text displayed in the text field.
 * @param textInputChange A lambda function invoked when there is a change in the text field value.
 * It takes the updated text value as a parameter.
 * @param contextText The optional context text displayed above the title.
 * @param maxLines The maximum number of lines for the text field.
 */
@Composable
private fun TextInputItem(
    title: String,
    defaultText: String,
    textInputChange: (String) -> Unit,
    contextText: String? = null,
    maxLines: Int = 1,
) {
    CommonPadding {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            contextText?.let {
                Text(it, style = TextStyle(fontSize = 10.sp))
            }

            if (contextText != null) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(title, style = TextStyle(fontSize = 10.sp))

            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = defaultText,
                    onValueChange = { newValue ->
                        textInputChange(newValue)
                    },
                    maxLines = maxLines,
                )
            }
        }
    }
}

/**
 * A composable function to represent a switch item, which consists of a title and a switch.
 *
 * @param title The title of the switch item.
 * @param isToggled The current state of the switch; whether it's toggled on or off.
 * @param onToggleChange A lambda function invoked when there is a change in the switch state.
 * It takes the updated switch state as a parameter.
 */
@Composable
private fun SwitchItem(
    title: String,
    isToggled: Boolean,
    onToggleChange: (Boolean) -> Unit,
) {
    CommonPadding {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title)
            Switch(
                checked = isToggled,
                onCheckedChange = onToggleChange,
            )
        }
    }
}

//endregion Generic Control Composables

@Composable
private fun CommonPadding(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        content()
        Divider()
    }
}

@Preview(showBackground = true)
@Composable
private fun GeneratorPreview() {
    BitwardenTheme {
        GeneratorScreen()
    }
}
