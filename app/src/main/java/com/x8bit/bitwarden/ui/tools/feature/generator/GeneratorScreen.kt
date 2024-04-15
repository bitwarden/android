@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.toDp
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.icon.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenPolicyWarningText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase.Companion.PASSPHRASE_MAX_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passcode.PasscodeType.Passphrase.Companion.PASSPHRASE_MIN_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceTypeOption
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Top level composable for the generator screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel(),
    onNavigateToPasswordHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    val snackbarHostState = remember { SnackbarHostState() }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            GeneratorEvent.NavigateToPasswordHistory -> onNavigateToPasswordHistory()

            is GeneratorEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    message = event.message(resources).toString(),
                    duration = SnackbarDuration.Short,
                )
            }

            is GeneratorEvent.NavigateToTooltip -> {
                intentManager.launchUri(
                    "https://bitwarden.com/help/generator/#username-types".toUri(),
                )
            }

            GeneratorEvent.NavigateBack -> onNavigateBack.invoke()
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

    val onUsernameOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit =
        remember(viewModel) {
            {
                viewModel.trySendAction(
                    GeneratorAction.MainType.Username.UsernameTypeOptionSelect(
                        it,
                    ),
                )
            }
        }

    val passwordHandlers = remember(viewModel) {
        PasswordHandlers.create(viewModel = viewModel)
    }

    val passphraseHandlers = remember(viewModel) {
        PassphraseHandlers.create(viewModel = viewModel)
    }

    val usernameTypeHandlers = remember(viewModel) {
        UsernameTypeHandlers.create(viewModel = viewModel)
    }

    val forwardedEmailAliasHandlers = remember(viewModel) {
        ForwardedEmailAliasHandlers.create(viewModel = viewModel)
    }

    val plusAddressedEmailHandlers = remember(viewModel) {
        PlusAddressedEmailHandlers.create(viewModel = viewModel)
    }

    val catchAllEmailHandlers = remember(viewModel) {
        CatchAllEmailHandlers.create(viewModel = viewModel)
    }

    val randomWordHandlers = remember(viewModel) {
        RandomWordHandlers.create(viewModel = viewModel)
    }

    val scrollBehavior = when (state.generatorMode) {
        GeneratorMode.Default -> {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        }

        else -> TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    }

    BitwardenScaffold(
        topBar = {
            when (state.generatorMode) {
                is GeneratorMode.Modal.Username,
                GeneratorMode.Modal.Password,
                -> {
                    ModalAppBar(
                        scrollBehavior = scrollBehavior,
                        onCloseClick = remember(viewModel) {
                            { viewModel.trySendAction(GeneratorAction.CloseClick) }
                        },
                        onSelectClick = remember(viewModel) {
                            { viewModel.trySendAction(GeneratorAction.SelectClick) }
                        },
                    )
                }

                GeneratorMode.Default -> {
                    DefaultAppBar(
                        scrollBehavior = scrollBehavior,
                        onPasswordHistoryClick = remember(viewModel) {
                            { viewModel.trySendAction(GeneratorAction.PasswordHistoryClick) }
                        },
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        ScrollContent(
            state = state,
            onRegenerateClick = onRegenerateClick,
            onCopyClick = onCopyClick,
            onMainStateOptionClicked = onMainStateOptionClicked,
            onPasscodeSubStateOptionClicked = onPasscodeOptionClicked,
            onUsernameSubStateOptionClicked = onUsernameOptionClicked,
            passwordHandlers = passwordHandlers,
            passphraseHandlers = passphraseHandlers,
            usernameTypeHandlers = usernameTypeHandlers,
            forwardedEmailAliasHandlers = forwardedEmailAliasHandlers,
            plusAddressedEmailHandlers = plusAddressedEmailHandlers,
            catchAllEmailHandlers = catchAllEmailHandlers,
            randomWordHandlers = randomWordHandlers,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

//region Top App Bar Composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onPasswordHistoryClick: () -> Unit,
) {
    BitwardenMediumTopAppBar(
        title = stringResource(id = R.string.generator),
        scrollBehavior = scrollBehavior,
        actions = {
            BitwardenOverflowActionItem(
                menuItemDataList = persistentListOf(
                    OverflowMenuItemData(
                        text = stringResource(id = R.string.password_history),
                        onClick = onPasswordHistoryClick,
                    ),
                ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onCloseClick: () -> Unit,
    onSelectClick: () -> Unit,
) {
    BitwardenTopAppBar(
        title = stringResource(id = R.string.generator),
        navigationIcon = painterResource(id = R.drawable.ic_close),
        navigationIconContentDescription = stringResource(id = R.string.close),
        onNavigationIconClick = onCloseClick,
        scrollBehavior = scrollBehavior,
        actions = {
            BitwardenTextButton(
                label = stringResource(id = R.string.select),
                onClick = onSelectClick,
                modifier = Modifier.semantics { testTag = "SelectButton" },
            )
        },
    )
}

//endregion Top App Bar Composables

//region ScrollContent and Static Items

@Suppress("LongMethod")
@Composable
private fun ScrollContent(
    state: GeneratorState,
    onRegenerateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
    onPasscodeSubStateOptionClicked: (GeneratorState.MainType.Passcode.PasscodeTypeOption) -> Unit,
    onUsernameSubStateOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit,
    passwordHandlers: PasswordHandlers,
    passphraseHandlers: PassphraseHandlers,
    usernameTypeHandlers: UsernameTypeHandlers,
    forwardedEmailAliasHandlers: ForwardedEmailAliasHandlers,
    plusAddressedEmailHandlers: PlusAddressedEmailHandlers,
    catchAllEmailHandlers: CatchAllEmailHandlers,
    randomWordHandlers: RandomWordHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {

        if (state.isUnderPolicy) {
            BitwardenPolicyWarningText(
                text = stringResource(id = R.string.password_generator_policy_in_effect),
                modifier = Modifier
                    .semantics { testTag = "PasswordGeneratorPolicyInEffectLabel" }
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        GeneratedStringItem(
            generatedText = state.generatedText,
            onCopyClick = onCopyClick,
            onRegenerateClick = onRegenerateClick,
        )

        if (state.generatorMode == GeneratorMode.Default) {
            Spacer(modifier = Modifier.height(8.dp))
            MainStateOptionsItem(
                selectedType = state.selectedType,
                possibleMainStates = state.typeOptions,
                onMainStateOptionClicked = onMainStateOptionClicked,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenListHeaderText(
            label = stringResource(id = R.string.options),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passcode -> {
                PasscodeTypeItems(
                    passcodeState = selectedType,
                    onSubStateOptionClicked = onPasscodeSubStateOptionClicked,
                    passwordHandlers = passwordHandlers,
                    passphraseHandlers = passphraseHandlers,
                )
            }

            is GeneratorState.MainType.Username -> {
                UsernameTypeItems(
                    usernameState = selectedType,
                    usernameTypeHandlers = usernameTypeHandlers,
                    onSubStateOptionClicked = onUsernameSubStateOptionClicked,
                    forwardedEmailAliasHandlers = forwardedEmailAliasHandlers,
                    plusAddressedEmailHandlers = plusAddressedEmailHandlers,
                    catchAllEmailHandlers = catchAllEmailHandlers,
                    randomWordHandlers = randomWordHandlers,
                )
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
    BitwardenTextFieldWithActions(
        label = "",
        textFieldTestTag = "GeneratedPasswordLabel",
        value = generatedText,
        singleLine = false,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = stringResource(id = R.string.copy),
                ),
                onClick = onCopyClick,
                modifier = Modifier.semantics { testTag = "CopyValueButton" },
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_generator),
                    contentDescription = stringResource(id = R.string.generate_password),
                ),
                onClick = onRegenerateClick,
                modifier = Modifier.semantics { testTag = "RegenerateValueButton" },
            )
        },
        onValueChange = {},
        readOnly = true,
        textStyle = LocalNonMaterialTypography.current.sensitiveInfoSmall,
        shouldAddCustomLineBreaks = true,
        visualTransformation = nonLetterColorVisualTransformation(),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun MainStateOptionsItem(
    selectedType: GeneratorState.MainType,
    possibleMainStates: List<GeneratorState.MainTypeOption>,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
) {
    val optionsWithStrings = possibleMainStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.what_would_you_like_to_generate),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onMainStateOptionClicked(selectedOptionId)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .semantics { testTag = "GeneratorTypePicker" },
    )
}

//endregion ScrollContent and Static Items

//region PasscodeType Composables

@Composable
private fun ColumnScope.PasscodeTypeItems(
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
    val possibleSubStates = GeneratorState.MainType.Passcode.PasscodeTypeOption.entries
    val optionsWithStrings = possibleSubStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.password_type),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = currentSubState.selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .semantics { testTag = "PasswordTypePicker" },
    )
}

//endregion PasscodeType Composables

//region PasswordType Composables

@Suppress("LongMethod")
@Composable
private fun ColumnScope.PasswordTypeContent(
    passwordTypeState: GeneratorState.MainType.Passcode.PasscodeType.Password,
    passwordHandlers: PasswordHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    PasswordLengthSliderItem(
        length = passwordTypeState.length,
        onPasswordSliderLengthChange = passwordHandlers.onPasswordSliderLengthChange,
        minValue = passwordTypeState.minLength,
        maxValue = passwordTypeState.maxLength,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {

        PasswordCapitalLettersToggleItem(
            useCapitals = passwordTypeState.useCapitals,
            onPasswordToggleCapitalLettersChange =
            passwordHandlers.onPasswordToggleCapitalLettersChange,
            enabled = passwordTypeState.capitalsEnabled,
        )
        PasswordLowercaseLettersToggleItem(
            useLowercase = passwordTypeState.useLowercase,
            onPasswordToggleLowercaseLettersChange =
            passwordHandlers.onPasswordToggleLowercaseLettersChange,
            enabled = passwordTypeState.lowercaseEnabled,
        )
        PasswordNumbersToggleItem(
            useNumbers = passwordTypeState.useNumbers,
            onPasswordToggleNumbersChange =
            passwordHandlers.onPasswordToggleNumbersChange,
            enabled = passwordTypeState.numbersEnabled,
        )
        PasswordSpecialCharactersToggleItem(
            useSpecialChars = passwordTypeState.useSpecialChars,
            onPasswordToggleSpecialCharactersChange =
            passwordHandlers.onPasswordToggleSpecialCharactersChange,
            enabled = passwordTypeState.specialCharsEnabled,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    PasswordMinNumbersCounterItem(
        minNumbers = passwordTypeState.minNumbers,
        onPasswordMinNumbersCounterChange =
        passwordHandlers.onPasswordMinNumbersCounterChange,
        maxValue = passwordTypeState.maxNumbersAllowed,
        minValue = passwordTypeState.minNumbersAllowed,
    )

    Spacer(modifier = Modifier.height(8.dp))

    PasswordMinSpecialCharactersCounterItem(
        minSpecial = passwordTypeState.minSpecial,
        onPasswordMinSpecialCharactersChange =
        passwordHandlers.onPasswordMinSpecialCharactersChange,
        maxValue = passwordTypeState.maxSpecialAllowed,
        minValue = passwordTypeState.minSpecialAllowed,
    )

    Spacer(modifier = Modifier.height(16.dp))

    PasswordAvoidAmbiguousCharsToggleItem(
        avoidAmbiguousChars = passwordTypeState.avoidAmbiguousChars,
        onPasswordToggleAvoidAmbiguousCharsChange =
        passwordHandlers.onPasswordToggleAvoidAmbiguousCharsChange,
        enabled = passwordTypeState.ambiguousCharsEnabled,
    )
}

@Composable
private fun PasswordLengthSliderItem(
    length: Int,
    onPasswordSliderLengthChange: (value: Int, isUserInteracting: Boolean) -> Unit,
    minValue: Int,
    maxValue: Int,
) {
    val sliderValue by rememberUpdatedState(newValue = length.coerceIn(minValue, maxValue))
    var labelTextWidth by remember { mutableStateOf(Dp.Unspecified) }

    val density = LocalDensity.current
    val sliderRange = minValue.toFloat()..maxValue.toFloat()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        OutlinedTextField(
            value = sliderValue.toString(),
            readOnly = true,
            onValueChange = { },
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
                .semantics { testTag = "PasswordLengthLabel" }
                .wrapContentWidth()
                .width(labelTextWidth + 16.dp + 16.dp),
        )

        Slider(
            value = sliderValue.toFloat(),
            onValueChange = { newValue ->
                onPasswordSliderLengthChange(newValue.toInt(), true)
            },
            onValueChangeFinished = {
                onPasswordSliderLengthChange(sliderValue, false)
            },
            valueRange = sliderRange,
            steps = maxValue - 1,
            colors = SliderDefaults.colors(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier
                .semantics { testTag = "PasswordLengthSlider" }
                .weight(1f),
        )
    }
}

@Composable
private fun PasswordCapitalLettersToggleItem(
    useCapitals: Boolean,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = "A—Z",
        isChecked = useCapitals,
        onCheckedChange = onPasswordToggleCapitalLettersChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "UppercaseAtoZToggle" }
            .padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.uppercase_ato_z),
    )
}

@Composable
private fun PasswordLowercaseLettersToggleItem(
    useLowercase: Boolean,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = "a—z",
        isChecked = useLowercase,
        onCheckedChange = onPasswordToggleLowercaseLettersChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "LowercaseAtoZToggle" }
            .padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.lowercase_ato_z),
    )
}

@Composable
private fun PasswordNumbersToggleItem(
    useNumbers: Boolean,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = "0-9",
        isChecked = useNumbers,
        onCheckedChange = onPasswordToggleNumbersChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "NumbersZeroToNineToggle" }
            .padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.numbers_zero_to_nine),
    )
}

@Composable
private fun PasswordSpecialCharactersToggleItem(
    useSpecialChars: Boolean,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = "!@#$%^&*",
        isChecked = useSpecialChars,
        onCheckedChange = onPasswordToggleSpecialCharactersChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "SpecialCharactersToggle" }
            .padding(horizontal = 16.dp),
        contentDescription = stringResource(id = R.string.special_characters),
    )
}

@Composable
private fun PasswordMinNumbersCounterItem(
    minNumbers: Int,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
) {
    BitwardenStepper(
        label = stringResource(id = R.string.min_numbers),
        value = minNumbers.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinNumbersCounterChange,
        increaseButtonTestTag = "MinNumberIncreaseButton",
        decreaseButtonTestTag = "MinNumberDecreaseButton",
        modifier = Modifier
            .semantics { testTag = "MinNumberValueLabel" }
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordMinSpecialCharactersCounterItem(
    minSpecial: Int,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
) {
    BitwardenStepper(
        label = stringResource(id = R.string.min_special),
        value = minSpecial.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinSpecialCharactersChange,
        increaseButtonTestTag = "MinSpecialIncreaseButton",
        decreaseButtonTestTag = "MinSpecialDecreaseButton",
        modifier = Modifier
            .semantics { testTag = "MinSpecialValueLabel" }
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordAvoidAmbiguousCharsToggleItem(
    avoidAmbiguousChars: Boolean,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.avoid_ambiguous_characters),
        isChecked = avoidAmbiguousChars,
        enabled = enabled,
        onCheckedChange = onPasswordToggleAvoidAmbiguousCharsChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "AvoidAmbiguousCharsToggle" }
            .padding(horizontal = 16.dp),
    )
}

//endregion PasswordType Composables

//region PassphraseType Composables

@Composable
private fun ColumnScope.PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passcode.PasscodeType.Passphrase,
    passphraseHandlers: PassphraseHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    PassphraseNumWordsCounterItem(
        numWords = passphraseTypeState.numWords,
        onPassphraseNumWordsCounterChange =
        passphraseHandlers.onPassphraseNumWordsCounterChange,
        minValue = passphraseTypeState.minNumWords,
        maxValue = passphraseTypeState.maxNumWords,
    )

    Spacer(modifier = Modifier.height(8.dp))

    PassphraseWordSeparatorInputItem(
        wordSeparator = passphraseTypeState.wordSeparator,
        onPassphraseWordSeparatorChange = passphraseHandlers.onPassphraseWordSeparatorChange,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        PassphraseCapitalizeToggleItem(
            capitalize = passphraseTypeState.capitalize,
            onPassphraseCapitalizeToggleChange =
            passphraseHandlers.onPassphraseCapitalizeToggleChange,
            enabled = passphraseTypeState.capitalizeEnabled,
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onPassphraseIncludeNumberToggleChange =
            passphraseHandlers.onPassphraseIncludeNumberToggleChange,
            enabled = passphraseTypeState.includeNumberEnabled,
        )
    }
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
    minValue: Int = PASSPHRASE_MIN_NUMBER_OF_WORDS,
    maxValue: Int = PASSPHRASE_MAX_NUMBER_OF_WORDS,
) {
    val coercedNumWords = numWords.coerceIn(minValue, maxValue)

    BitwardenStepper(
        label = stringResource(id = R.string.number_of_words),
        value = coercedNumWords,
        range = minValue..maxValue,
        onValueChange = onPassphraseNumWordsCounterChange,
        stepperActionsTestTag = "NumberOfWordsStepper",
        increaseButtonTestTag = "NumberOfWordsIncreaseButton",
        decreaseButtonTestTag = "NumberOfWordsDecreaseButton",
        modifier = Modifier
            .semantics { testTag = "NumberOfWordsLabel" }
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onPassphraseWordSeparatorChange: (wordSeparator: Char?) -> Unit,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.word_separator),
        value = wordSeparator?.toString().orEmpty(),
        onValueChange = {
            // When onValueChange triggers and we don't update the value for whatever reason,
            // onValueChange triggers again with the previous value.
            // To avoid passphrase regeneration, we filter out those re-emissions.
            val char = it.firstOrNull()
            if (char != wordSeparator) {
                onPassphraseWordSeparatorChange(char)
            }
        },
        modifier = Modifier
            .semantics { testTag = "WordSeparatorEntry" }
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PassphraseCapitalizeToggleItem(
    capitalize: Boolean,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.capitalize),
        isChecked = capitalize,
        onCheckedChange = onPassphraseCapitalizeToggleChange,
        enabled = enabled,
        modifier = Modifier
            .semantics { testTag = "CapitalizePassphraseToggle" }
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PassphraseIncludeNumberToggleItem(
    includeNumber: Boolean,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        enabled = enabled,
        onCheckedChange = onPassphraseIncludeNumberToggleChange,
        modifier = Modifier
            .semantics { testTag = "IncludeNumbersToggle" }
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

//endregion PassphraseType Composables

//region UsernameType Composables

@Composable
private fun ColumnScope.UsernameTypeItems(
    usernameState: GeneratorState.MainType.Username,
    onSubStateOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit,
    usernameTypeHandlers: UsernameTypeHandlers,
    forwardedEmailAliasHandlers: ForwardedEmailAliasHandlers,
    plusAddressedEmailHandlers: PlusAddressedEmailHandlers,
    catchAllEmailHandlers: CatchAllEmailHandlers,
    randomWordHandlers: RandomWordHandlers,
) {
    UsernameOptionsItem(usernameState, onSubStateOptionClicked, usernameTypeHandlers)

    when (val selectedType = usernameState.selectedType) {
        is GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail -> {
            PlusAddressedEmailTypeContent(
                usernameTypeState = selectedType,
                plusAddressedEmailHandlers = plusAddressedEmailHandlers,
            )
        }

        is GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias -> {
            ForwardedEmailAliasTypeContent(
                usernameTypeState = selectedType,
                forwardedEmailAliasHandlers = forwardedEmailAliasHandlers,
            )
        }

        is GeneratorState.MainType.Username.UsernameType.CatchAllEmail -> {
            CatchAllEmailTypeContent(
                usernameTypeState = selectedType,
                catchAllEmailHandlers = catchAllEmailHandlers,
            )
        }

        is GeneratorState.MainType.Username.UsernameType.RandomWord -> {
            RandomWordTypeContent(
                randomWordTypeState = selectedType,
                randomWordHandlers = randomWordHandlers,
            )
        }
    }
}

@Composable
private fun UsernameOptionsItem(
    currentSubState: GeneratorState.MainType.Username,
    onSubStateOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit,
    usernameTypeHandlers: UsernameTypeHandlers,
) {
    val possibleSubStates = GeneratorState.MainType.Username.UsernameTypeOption.entries
    val optionsWithStrings = possibleSubStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.username_type),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = currentSubState.selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .semantics { testTag = "UsernameTypePicker" },
        supportingText = currentSubState.selectedType.supportingStringResId?.let {
            stringResource(id = it)
        },
        tooltip = TooltipData(
            onClick = usernameTypeHandlers.onUsernameTooltipClicked,
            contentDescription = stringResource(id = R.string.learn_more),
        ),
    )
}

//endregion UsernameType Composables

//region ForwardedEmailAliasType Composables

@Suppress("LongMethod")
@Composable
private fun ColumnScope.ForwardedEmailAliasTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias,
    forwardedEmailAliasHandlers: ForwardedEmailAliasHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    ServiceTypeOptionsItem(
        currentSubState = usernameTypeState,
        onSubStateOptionClicked = forwardedEmailAliasHandlers.onServiceChange,
    )

    Spacer(modifier = Modifier.height(8.dp))

    when (usernameTypeState.selectedServiceType) {

        is ServiceType.AddyIo -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_access_token),
                value = usernameTypeState.selectedServiceType.apiAccessToken,
                onValueChange = forwardedEmailAliasHandlers.onAddyIoAccessTokenTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.domain_name_required_parenthesis),
                value = usernameTypeState.selectedServiceType.domainName,
                onValueChange = forwardedEmailAliasHandlers.onAddyIoDomainNameTextChange,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "AnonAddyDomainNameEntry" }
                    .fillMaxWidth(),
            )
        }

        is ServiceType.DuckDuckGo -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_key_required_parenthesis),
                value = usernameTypeState.selectedServiceType.apiKey,
                onValueChange = forwardedEmailAliasHandlers.onDuckDuckGoApiKeyTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )
        }

        is ServiceType.FastMail -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_key_required_parenthesis),
                value = usernameTypeState.selectedServiceType.apiKey,
                onValueChange = forwardedEmailAliasHandlers.onFastMailApiKeyTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )
        }

        is ServiceType.FirefoxRelay -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_access_token),
                value = usernameTypeState.selectedServiceType.apiAccessToken,
                onValueChange = forwardedEmailAliasHandlers.onFirefoxRelayAccessTokenTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )
        }

        is ServiceType.ForwardEmail -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_key_required_parenthesis),
                value = usernameTypeState.selectedServiceType.apiKey,
                onValueChange = forwardedEmailAliasHandlers.onForwardEmailApiKeyTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.domain_name_required_parenthesis),
                value = usernameTypeState.selectedServiceType.domainName,
                onValueChange = forwardedEmailAliasHandlers.onForwardEmailDomainNameTextChange,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailDomainNameEntry" }
                    .fillMaxWidth(),
            )
        }

        is ServiceType.SimpleLogin -> {
            BitwardenPasswordField(
                label = stringResource(id = R.string.api_key_required_parenthesis),
                value = usernameTypeState.selectedServiceType.apiKey,
                onValueChange = forwardedEmailAliasHandlers.onSimpleLoginApiKeyTextChange,
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )
        }

        null -> {
            var obfuscatedTextField by remember { mutableStateOf("") }
            BitwardenPasswordField(
                label = "",
                value = obfuscatedTextField,
                onValueChange = { obfuscatedTextField = it },
                showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { testTag = "ForwardedEmailApiSecretEntry" }
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ServiceTypeOptionsItem(
    currentSubState: GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias,
    onSubStateOptionClicked: (ServiceTypeOption) -> Unit,
) {
    val possibleSubStates = ServiceTypeOption.entries
    val optionsWithStrings = possibleSubStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.service),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = (currentSubState.selectedServiceType?.displayStringResId)?.let {
            stringResource(id = it)
        },
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .semantics { testTag = "ServiceTypePicker" }
            .fillMaxWidth(),
    )
}

//region PlusAddressedEmailType Composables

@Composable
private fun ColumnScope.PlusAddressedEmailTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail,
    plusAddressedEmailHandlers: PlusAddressedEmailHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    PlusAddressedEmailTextInputItem(
        email = usernameTypeState.email,
        onPlusAddressedEmailTextChange = plusAddressedEmailHandlers.onEmailChange,
    )
}

@Composable
private fun PlusAddressedEmailTextInputItem(
    email: String,
    onPlusAddressedEmailTextChange: (email: String) -> Unit,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.email_required_parenthesis),
        value = email,
        onValueChange = {
            onPlusAddressedEmailTextChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "PlusAddressedEmailEntry" }
            .padding(horizontal = 16.dp),
    )
}

//endregion PlusAddressedEmailType Composables

//region CatchAllEmailType Composables

@Composable
private fun ColumnScope.CatchAllEmailTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.CatchAllEmail,
    catchAllEmailHandlers: CatchAllEmailHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    CatchAllEmailTextInputItem(
        domain = usernameTypeState.domainName,
        onDomainTextChange = catchAllEmailHandlers.onDomainChange,
    )
}

@Composable
private fun CatchAllEmailTextInputItem(
    domain: String,
    onDomainTextChange: (domain: String) -> Unit,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.domain_name_required_parenthesis),
        value = domain,
        onValueChange = {
            onDomainTextChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "CatchAllEmailDomainEntry" }
            .padding(horizontal = 16.dp),
    )
}

//endregion CatchAllEmailType Composables

//region Random Word Composables

@Composable
private fun ColumnScope.RandomWordTypeContent(
    randomWordTypeState: GeneratorState.MainType.Username.UsernameType.RandomWord,
    randomWordHandlers: RandomWordHandlers,
) {
    Spacer(modifier = Modifier.height(16.dp))

    RandomWordCapitalizeToggleItem(
        capitalize = randomWordTypeState.capitalize,
        onRandomWordCapitalizeToggleChange = randomWordHandlers.onCapitalizeChange,
    )

    RandomWordIncludeNumberToggleItem(
        includeNumber = randomWordTypeState.includeNumber,
        onRandomWordIncludeNumberToggleChange = randomWordHandlers.onIncludeNumberChange,
    )
}

@Composable
private fun RandomWordCapitalizeToggleItem(
    capitalize: Boolean,
    onRandomWordCapitalizeToggleChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.capitalize),
        isChecked = capitalize,
        onCheckedChange = onRandomWordCapitalizeToggleChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "CapitalizeRandomWordUsernameToggle" }
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun RandomWordIncludeNumberToggleItem(
    includeNumber: Boolean,
    onRandomWordIncludeNumberToggleChange: (Boolean) -> Unit,
) {
    BitwardenWideSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        onCheckedChange = onRandomWordIncludeNumberToggleChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "IncludeNumberRandomWordUsernameToggle" }
            .padding(horizontal = 16.dp),
    )
}

//endregion Random Word Composables

@Preview(showBackground = true)
@Composable
private fun GeneratorPreview() {
    BitwardenTheme {
        GeneratorScreen(
            onNavigateToPasswordHistory = {},
            onNavigateBack = {},
        )
    }
}

/**
 * A class dedicated to handling user interactions related to password configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
private data class PasswordHandlers(
    val onPasswordSliderLengthChange: (Int, Boolean) -> Unit,
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
                onPasswordSliderLengthChange = { newLength, isUserInteracting ->
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Passcode.PasscodeType.Password
                            .SliderLengthChange(
                                length = newLength,
                                isUserInteracting = isUserInteracting,
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
private data class PassphraseHandlers(
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

/**
 * A class dedicated to handling user interactions related to all username configurations.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
private data class UsernameTypeHandlers(
    val onUsernameTooltipClicked: () -> Unit,
) {
    companion object {
        fun create(viewModel: GeneratorViewModel): UsernameTypeHandlers {
            return UsernameTypeHandlers(
                onUsernameTooltipClicked = {
                    viewModel.trySendAction(
                        GeneratorAction.MainType.Username.UsernameType.TooltipClick,
                    )
                },
            )
        }
    }
}

/**
 * A class dedicated to handling user interactions related to forwarded email alias
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
private data class ForwardedEmailAliasHandlers(
    val onServiceChange: (ServiceTypeOption) -> Unit,
    val onAddyIoAccessTokenTextChange: (String) -> Unit,
    val onAddyIoDomainNameTextChange: (String) -> Unit,
    val onDuckDuckGoApiKeyTextChange: (String) -> Unit,
    val onFastMailApiKeyTextChange: (String) -> Unit,
    val onFirefoxRelayAccessTokenTextChange: (String) -> Unit,
    val onForwardEmailApiKeyTextChange: (String) -> Unit,
    val onForwardEmailDomainNameTextChange: (String) -> Unit,
    val onSimpleLoginApiKeyTextChange: (String) -> Unit,
) {
    companion object {
        @Suppress("LongMethod")
        fun create(viewModel: GeneratorViewModel): ForwardedEmailAliasHandlers {
            return ForwardedEmailAliasHandlers(
                onServiceChange = { newServiceTypeOption ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .ServiceTypeOptionSelect(
                                serviceTypeOption = newServiceTypeOption,
                            ),
                    )
                },
                onAddyIoAccessTokenTextChange = { newAccessToken ->
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
                },
                onAddyIoDomainNameTextChange = { newDomainName ->
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
                },
                onDuckDuckGoApiKeyTextChange = { newApiKey ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .DuckDuckGo
                            .ApiKeyTextChange(
                                apiKey = newApiKey,
                            ),
                    )
                },
                onFastMailApiKeyTextChange = { newApiKey ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .FastMail
                            .ApiKeyTextChange(
                                apiKey = newApiKey,
                            ),
                    )
                },
                onFirefoxRelayAccessTokenTextChange = { newAccessToken ->
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
                },
                onForwardEmailApiKeyTextChange = { newApiKey ->
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
                },
                onForwardEmailDomainNameTextChange = { newDomainName ->
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
                },
                onSimpleLoginApiKeyTextChange = { newApiKey ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .ForwardedEmailAlias
                            .SimpleLogin
                            .ApiKeyTextChange(
                                apiKey = newApiKey,
                            ),
                    )
                },
            )
        }
    }
}

/**
 * A class dedicated to handling user interactions related to plus addressed email
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
private data class PlusAddressedEmailHandlers(
    val onEmailChange: (String) -> Unit,
) {
    companion object {
        fun create(viewModel: GeneratorViewModel): PlusAddressedEmailHandlers {
            return PlusAddressedEmailHandlers(
                onEmailChange = { newEmail ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .PlusAddressedEmail
                            .EmailTextChange(
                                email = newEmail,
                            ),
                    )
                },
            )
        }
    }
}

/**
 * A class dedicated to handling user interactions related to plus addressed email
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
private data class CatchAllEmailHandlers(
    val onDomainChange: (String) -> Unit,
) {
    companion object {
        fun create(viewModel: GeneratorViewModel): CatchAllEmailHandlers {
            return CatchAllEmailHandlers(
                onDomainChange = { newDomain ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .CatchAllEmail
                            .DomainTextChange(
                                domain = newDomain,
                            ),
                    )
                },
            )
        }
    }
}

/**
 * A class dedicated to handling user interactions related to Random Word
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
private data class RandomWordHandlers(
    val onCapitalizeChange: (Boolean) -> Unit,
    val onIncludeNumberChange: (Boolean) -> Unit,
) {
    companion object {
        fun create(viewModel: GeneratorViewModel): RandomWordHandlers {
            return RandomWordHandlers(
                onCapitalizeChange = { shouldCapitalize ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .RandomWord
                            .ToggleCapitalizeChange(
                                capitalize = shouldCapitalize,
                            ),
                    )
                },
                onIncludeNumberChange = { shouldIncludeNumber ->
                    viewModel.trySendAction(
                        GeneratorAction
                            .MainType
                            .Username
                            .UsernameType
                            .RandomWord
                            .ToggleIncludeNumberChange(
                                includeNumber = shouldIncludeNumber,
                            ),
                    )
                },
            )
        }
    }
}
