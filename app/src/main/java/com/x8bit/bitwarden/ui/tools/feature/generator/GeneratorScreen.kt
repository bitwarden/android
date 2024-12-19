@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.LivecycleEventEffect
import com.x8bit.bitwarden.ui.platform.base.util.scrolledContainerBottomDivider
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTonalIconButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.TextToolbarType
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.model.TopAppBarDividerStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.segment.BitwardenSegmentedButton
import com.x8bit.bitwarden.ui.platform.components.segment.SegmentedButtonState
import com.x8bit.bitwarden.ui.platform.components.slider.BitwardenSlider
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.x8bit.bitwarden.ui.platform.components.snackbar.rememberBitwardenSnackbarHostState
import com.x8bit.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passphrase.Companion.PASSPHRASE_MAX_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Passphrase.Companion.PASSPHRASE_MIN_NUMBER_OF_WORDS
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceTypeOption
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.CatchAllEmailHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.ForwardedEmailAliasHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.PassphraseHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.PasswordHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.PlusAddressedEmailHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.RandomWordHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.UsernameTypeHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberCatchAllEmailHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberForwardedEmailAliasHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberPassphraseHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberPasswordHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberPlusAddressedEmailHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberRandomWordHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.handlers.rememberUsernameTypeHandlers
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max

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
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    LivecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.trySendAction(GeneratorAction.LifecycleResume)
            }

            else -> Unit
        }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            GeneratorEvent.NavigateToPasswordHistory -> onNavigateToPasswordHistory()

            is GeneratorEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    snackbarData = BitwardenSnackbarData(message = event.message),
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

    val onUsernameOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit =
        remember(viewModel) {
            {
                viewModel.trySendAction(
                    GeneratorAction.MainType.Username.UsernameTypeOptionSelect(it),
                )
            }
        }

    val passwordHandlers = rememberPasswordHandlers(viewModel)
    val passphraseHandlers = rememberPassphraseHandlers(viewModel)
    val usernameTypeHandlers = rememberUsernameTypeHandlers(viewModel)
    val forwardedEmailAliasHandlers = rememberForwardedEmailAliasHandlers(viewModel)
    val plusAddressedEmailHandlers = rememberPlusAddressedEmailHandlers(viewModel)
    val catchAllEmailHandlers = rememberCatchAllEmailHandlers(viewModel)
    val randomWordHandlers = rememberRandomWordHandlers(viewModel)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            when (val generatorMode = state.generatorMode) {
                is GeneratorMode.Modal -> {
                    ModalAppBar(
                        generatorMode = generatorMode,
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
        utilityBar = {
            MainStateOptionsItem(
                selectedType = state.selectedType,
                passcodePolicyOverride = state.passcodePolicyOverride,
                possibleMainStates = state.typeOptions.toImmutableList(),
                onMainStateOptionClicked = onMainStateOptionClicked,
                modifier = Modifier
                    .scrolledContainerBottomDivider(topAppBarScrollBehavior = scrollBehavior),
            )
        },
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        ScrollContent(
            state = state,
            onRegenerateClick = onRegenerateClick,
            onCopyClick = onCopyClick,
            onUsernameSubStateOptionClicked = onUsernameOptionClicked,
            passwordHandlers = passwordHandlers,
            passphraseHandlers = passphraseHandlers,
            usernameTypeHandlers = usernameTypeHandlers,
            forwardedEmailAliasHandlers = forwardedEmailAliasHandlers,
            plusAddressedEmailHandlers = plusAddressedEmailHandlers,
            catchAllEmailHandlers = catchAllEmailHandlers,
            randomWordHandlers = randomWordHandlers,
        )
    }
}

//region Top App Bar Composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onPasswordHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenMediumTopAppBar(
        title = stringResource(id = R.string.generator),
        scrollBehavior = scrollBehavior,
        dividerStyle = TopAppBarDividerStyle.NONE,
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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalAppBar(
    generatorMode: GeneratorMode.Modal,
    scrollBehavior: TopAppBarScrollBehavior,
    onCloseClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTopAppBar(
        title = stringResource(id = R.string.generator),
        navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
        navigationIconContentDescription = stringResource(id = R.string.close),
        onNavigationIconClick = onCloseClick,
        scrollBehavior = scrollBehavior,
        dividerStyle = when (generatorMode) {
            GeneratorMode.Modal.Password -> TopAppBarDividerStyle.NONE
            is GeneratorMode.Modal.Username -> TopAppBarDividerStyle.ON_SCROLL
        },
        actions = {
            BitwardenTextButton(
                label = stringResource(id = R.string.select),
                onClick = onSelectClick,
                modifier = Modifier.testTag("SelectButton"),
            )
        },
        modifier = modifier,
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
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenInfoCalloutCard(
                text = stringResource(id = R.string.password_generator_policy_in_effect),
                modifier = Modifier
                    .testTag("PasswordGeneratorPolicyInEffectLabel")
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

        Spacer(modifier = Modifier.height(16.dp))

        BitwardenListHeaderText(
            label = stringResource(id = R.string.options),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passphrase -> {
                PassphraseTypeContent(
                    passphraseTypeState = selectedType,
                    passphraseHandlers = passphraseHandlers,
                )
            }

            is GeneratorState.MainType.Password -> {
                PasswordTypeContent(
                    passwordTypeState = selectedType,
                    passwordHandlers = passwordHandlers,
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
    modifier: Modifier = Modifier,
) {
    BitwardenTextFieldWithActions(
        label = "",
        textFieldTestTag = "GeneratedPasswordLabel",
        value = generatedText,
        singleLine = false,
        actions = {
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_copy,
                contentDescription = stringResource(id = R.string.copy),
                onClick = onCopyClick,
                modifier = Modifier.testTag("CopyValueButton"),
            )
            BitwardenTonalIconButton(
                vectorIconRes = R.drawable.ic_generate,
                contentDescription = stringResource(id = R.string.generate_password),
                onClick = onRegenerateClick,
                modifier = Modifier.testTag("RegenerateValueButton"),
            )
        },
        onValueChange = {},
        readOnly = true,
        textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
        shouldAddCustomLineBreaks = true,
        visualTransformation = nonLetterColorVisualTransformation(),
        modifier = modifier.padding(horizontal = 16.dp),
        textToolbarType = TextToolbarType.NONE,
    )
}

@Composable
private fun MainStateOptionsItem(
    selectedType: GeneratorState.MainType,
    passcodePolicyOverride: GeneratorState.PasscodePolicyOverride?,
    possibleMainStates: ImmutableList<GeneratorState.MainTypeOption>,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenSegmentedButton(
        options = possibleMainStates
            .map { mainOptionType ->
                SegmentedButtonState(
                    text = stringResource(id = mainOptionType.labelRes),
                    onClick = { onMainStateOptionClicked(mainOptionType) },
                    isChecked = selectedType.mainTypeOption == mainOptionType,
                    isEnabled = when (mainOptionType) {
                        GeneratorState.MainTypeOption.PASSWORD -> {
                            when (passcodePolicyOverride) {
                                GeneratorState.PasscodePolicyOverride.PASSWORD -> true
                                GeneratorState.PasscodePolicyOverride.PASSPHRASE -> false
                                null -> true
                            }
                        }

                        GeneratorState.MainTypeOption.PASSPHRASE -> {
                            when (passcodePolicyOverride) {
                                GeneratorState.PasscodePolicyOverride.PASSWORD -> false
                                GeneratorState.PasscodePolicyOverride.PASSPHRASE -> true
                                null -> true
                            }
                        }

                        GeneratorState.MainTypeOption.USERNAME -> true
                    },
                    testTag = mainOptionType.testTag,
                )
            }
            .toImmutableList(),
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag = "GeneratorTypePicker"),
    )
}

//endregion ScrollContent and Static Items

//region PasswordType Composables

@Suppress("LongMethod")
@Composable
private fun ColumnScope.PasswordTypeContent(
    passwordTypeState: GeneratorState.MainType.Password,
    passwordHandlers: PasswordHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    BitwardenSlider(
        value = passwordTypeState.length,
        onValueChange = { newValue, isUserInteracting ->
            if (newValue >= passwordTypeState.computedMinimumLength) {
                passwordHandlers.onPasswordSliderLengthChange(newValue, isUserInteracting)
            }
        },
        range = passwordTypeState.minLength..passwordTypeState.maxLength,
        sliderTag = "PasswordLengthSlider",
        valueTag = "PasswordLengthLabel",
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 28.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {

        PasswordCapitalLettersToggleItem(
            useCapitals = passwordTypeState.useCapitals,
            onPasswordToggleCapitalLettersChange = passwordHandlers
                .onPasswordToggleCapitalLettersChange,
            enabled = passwordTypeState.capitalsEnabled,
        )
        PasswordLowercaseLettersToggleItem(
            useLowercase = passwordTypeState.useLowercase,
            onPasswordToggleLowercaseLettersChange = passwordHandlers
                .onPasswordToggleLowercaseLettersChange,
            enabled = passwordTypeState.lowercaseEnabled,
        )
        PasswordNumbersToggleItem(
            useNumbers = passwordTypeState.useNumbers,
            onPasswordToggleNumbersChange = passwordHandlers.onPasswordToggleNumbersChange,
            enabled = passwordTypeState.numbersEnabled,
        )
        PasswordSpecialCharactersToggleItem(
            useSpecialChars = passwordTypeState.useSpecialChars,
            onPasswordToggleSpecialCharactersChange = passwordHandlers
                .onPasswordToggleSpecialCharactersChange,
            enabled = passwordTypeState.specialCharsEnabled,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    PasswordMinNumbersCounterItem(
        minNumbers = passwordTypeState.minNumbers,
        onPasswordMinNumbersCounterChange = passwordHandlers.onPasswordMinNumbersCounterChange,
        maxValue = max(passwordTypeState.maxNumbersAllowed, passwordTypeState.minNumbersAllowed),
        minValue = passwordTypeState.minNumbersAllowed,
    )

    Spacer(modifier = Modifier.height(8.dp))

    PasswordMinSpecialCharactersCounterItem(
        minSpecial = passwordTypeState.minSpecial,
        onPasswordMinSpecialCharactersChange = passwordHandlers
            .onPasswordMinSpecialCharactersChange,
        maxValue = max(passwordTypeState.maxSpecialAllowed, passwordTypeState.minSpecialAllowed),
        minValue = passwordTypeState.minSpecialAllowed,
    )

    Spacer(modifier = Modifier.height(16.dp))

    PasswordAvoidAmbiguousCharsToggleItem(
        avoidAmbiguousChars = passwordTypeState.avoidAmbiguousChars,
        onPasswordToggleAvoidAmbiguousCharsChange = passwordHandlers
            .onPasswordToggleAvoidAmbiguousCharsChange,
        enabled = passwordTypeState.ambiguousCharsEnabled,
    )
}

@Composable
private fun PasswordCapitalLettersToggleItem(
    useCapitals: Boolean,
    onPasswordToggleCapitalLettersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = "A—Z",
        contentDescription = stringResource(id = R.string.uppercase_ato_z),
        isChecked = useCapitals,
        onCheckedChange = onPasswordToggleCapitalLettersChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("UppercaseAtoZToggle")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordLowercaseLettersToggleItem(
    useLowercase: Boolean,
    onPasswordToggleLowercaseLettersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = "a—z",
        contentDescription = stringResource(id = R.string.lowercase_ato_z),
        isChecked = useLowercase,
        onCheckedChange = onPasswordToggleLowercaseLettersChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("LowercaseAtoZToggle")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordNumbersToggleItem(
    useNumbers: Boolean,
    onPasswordToggleNumbersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = "0-9",
        contentDescription = stringResource(id = R.string.numbers_zero_to_nine),
        isChecked = useNumbers,
        onCheckedChange = onPasswordToggleNumbersChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("NumbersZeroToNineToggle")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordSpecialCharactersToggleItem(
    useSpecialChars: Boolean,
    onPasswordToggleSpecialCharactersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = "!@#$%^&*",
        contentDescription = stringResource(id = R.string.special_characters),
        isChecked = useSpecialChars,
        onCheckedChange = onPasswordToggleSpecialCharactersChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .testTag("SpecialCharactersToggle")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordMinNumbersCounterItem(
    minNumbers: Int,
    onPasswordMinNumbersCounterChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    BitwardenStepper(
        label = stringResource(id = R.string.min_numbers),
        value = minNumbers.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinNumbersCounterChange,
        modifier = modifier
            .testTag("MinNumberValueLabel")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordMinSpecialCharactersCounterItem(
    minSpecial: Int,
    onPasswordMinSpecialCharactersChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
) {
    BitwardenStepper(
        label = stringResource(id = R.string.min_special),
        value = minSpecial.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinSpecialCharactersChange,
        modifier = modifier
            .testTag("MinSpecialValueLabel")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PasswordAvoidAmbiguousCharsToggleItem(
    avoidAmbiguousChars: Boolean,
    onPasswordToggleAvoidAmbiguousCharsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = stringResource(id = R.string.avoid_ambiguous_characters),
        isChecked = avoidAmbiguousChars,
        enabled = enabled,
        onCheckedChange = onPasswordToggleAvoidAmbiguousCharsChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("AvoidAmbiguousCharsToggle")
            .padding(horizontal = 16.dp),
    )
}

//endregion PasswordType Composables

//region PassphraseType Composables

@Composable
private fun ColumnScope.PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passphrase,
    passphraseHandlers: PassphraseHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))

    PassphraseNumWordsCounterItem(
        numWords = passphraseTypeState.numWords,
        onPassphraseNumWordsCounterChange = passphraseHandlers.onPassphraseNumWordsCounterChange,
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
            onPassphraseCapitalizeToggleChange = passphraseHandlers
                .onPassphraseCapitalizeToggleChange,
            enabled = passphraseTypeState.capitalizeEnabled,
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onPassphraseIncludeNumberToggleChange = passphraseHandlers
                .onPassphraseIncludeNumberToggleChange,
            enabled = passphraseTypeState.includeNumberEnabled,
        )
    }
}

@Composable
private fun PassphraseNumWordsCounterItem(
    numWords: Int,
    onPassphraseNumWordsCounterChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .testTag("NumberOfWordsLabel")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onPassphraseWordSeparatorChange: (wordSeparator: Char?) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textFieldTestTag = "WordSeparatorEntry",
    )
}

@Composable
private fun PassphraseCapitalizeToggleItem(
    capitalize: Boolean,
    onPassphraseCapitalizeToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BitwardenSwitch(
        label = stringResource(id = R.string.capitalize),
        isChecked = capitalize,
        onCheckedChange = onPassphraseCapitalizeToggleChange,
        enabled = enabled,
        modifier = modifier
            .testTag("CapitalizePassphraseToggle")
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun PassphraseIncludeNumberToggleItem(
    includeNumber: Boolean,
    onPassphraseIncludeNumberToggleChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        enabled = enabled,
        onCheckedChange = onPassphraseIncludeNumberToggleChange,
        modifier = modifier
            .testTag("IncludeNumbersToggle")
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
    modifier: Modifier = Modifier,
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
        supportingText = currentSubState.selectedType.supportingStringResId?.let {
            stringResource(id = it)
        },
        tooltip = TooltipData(
            onClick = usernameTypeHandlers.onUsernameTooltipClicked,
            contentDescription = stringResource(id = R.string.learn_more),
        ),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .testTag("UsernameTypePicker"),
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
                    .testTag("ForwardedEmailApiSecretEntry")
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.domain_name_required_parenthesis),
                value = usernameTypeState.selectedServiceType.domainName,
                onValueChange = forwardedEmailAliasHandlers.onAddyIoDomainNameTextChange,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                textFieldTestTag = "AnonAddyDomainNameEntry",
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
                    .testTag("ForwardedEmailApiSecretEntry")
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
                    .testTag("ForwardedEmailApiSecretEntry")
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
                    .testTag("ForwardedEmailApiSecretEntry")
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
                    .testTag("ForwardedEmailApiSecretEntry")
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.domain_name_required_parenthesis),
                value = usernameTypeState.selectedServiceType.domainName,
                onValueChange = forwardedEmailAliasHandlers.onForwardEmailDomainNameTextChange,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                textFieldTestTag = "ForwardedEmailDomainNameEntry",
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
                    .testTag("ForwardedEmailApiSecretEntry")
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
                    .testTag("ForwardedEmailApiSecretEntry")
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ServiceTypeOptionsItem(
    currentSubState: GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias,
    onSubStateOptionClicked: (ServiceTypeOption) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .padding(horizontal = 16.dp)
            .testTag("ServiceTypePicker")
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
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.email_required_parenthesis),
        value = email,
        onValueChange = onPlusAddressedEmailTextChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textFieldTestTag = "PlusAddressedEmailEntry",
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
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.domain_name_required_parenthesis),
        value = domain,
        onValueChange = onDomainTextChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textFieldTestTag = "CatchAllEmailDomainEntry",
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
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = stringResource(id = R.string.capitalize),
        isChecked = capitalize,
        onCheckedChange = onRandomWordCapitalizeToggleChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("CapitalizeRandomWordUsernameToggle")
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun RandomWordIncludeNumberToggleItem(
    includeNumber: Boolean,
    onRandomWordIncludeNumberToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = stringResource(id = R.string.include_number),
        isChecked = includeNumber,
        onCheckedChange = onRandomWordIncludeNumberToggleChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("IncludeNumberRandomWordUsernameToggle")
            .padding(horizontal = 16.dp),
    )
}

//endregion Random Word Composables

@Preview(showBackground = true)
@Composable
private fun Generator_preview() {
    BitwardenTheme {
        GeneratorScreen(
            onNavigateToPasswordHistory = {},
            onNavigateBack = {},
        )
    }
}
