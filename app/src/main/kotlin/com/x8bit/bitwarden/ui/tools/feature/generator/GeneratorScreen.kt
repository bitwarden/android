@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.scrolledContainerBottomDivider
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenMediumTopAppBar
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.appbar.model.TopAppBarDividerStyle
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.coachmark.CoachMarkActionText
import com.bitwarden.ui.platform.components.coachmark.CoachMarkContainer
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkHighlightShape
import com.bitwarden.ui.platform.components.coachmark.model.rememberLazyListCoachMarkState
import com.bitwarden.ui.platform.components.coachmark.scope.CoachMarkScope
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.field.model.TextToolbarType
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.segment.BitwardenSegmentedButton
import com.bitwarden.ui.platform.components.segment.SegmentedButtonOptionContent
import com.bitwarden.ui.platform.components.segment.SegmentedButtonState
import com.bitwarden.ui.platform.components.slider.BitwardenSlider
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.RegisterScreenDataOnLifecycleEffect
import com.x8bit.bitwarden.ui.platform.composition.LocalAppResumeStateManager
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
import com.x8bit.bitwarden.ui.tools.feature.generator.model.ExploreGeneratorCoachMark
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
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
    onDimNavBarRequest: (Boolean) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    appResumeStateManager: AppResumeStateManager = LocalAppResumeStateManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.trySendAction(GeneratorAction.LifecycleResume)
            }

            else -> Unit
        }
    }
    RegisterScreenDataOnLifecycleEffect(
        appResumeStateManager = appResumeStateManager,
    ) {
        AppResumeScreenData.GeneratorScreen
    }

    val lazyListState = rememberLazyListState()
    val coachMarkState = rememberLazyListCoachMarkState(
        orderedList = ExploreGeneratorCoachMark.entries,
        lazyListState = lazyListState,
    )

    LaunchedEffect(key1 = coachMarkState.isVisible.value) {
        onDimNavBarRequest(coachMarkState.isVisible.value)
    }
    val scope = rememberCoroutineScope()
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
            GeneratorEvent.StartCoachMarkTour -> {
                scope.launch {
                    coachMarkState.showCoachMark(ExploreGeneratorCoachMark.PASSWORD_MODE)
                }
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

    val onUsernameOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit =
        remember(viewModel) {
            {
                viewModel.trySendAction(
                    GeneratorAction.MainType.Username.UsernameTypeOptionSelect(it),
                )
            }
        }

    val onShowNextCoachMark: () -> Unit = remember {
        { scope.launch { coachMarkState.showNextCoachMark() } }
    }
    val onShowPreviousCoachMark: () -> Unit = remember {
        { scope.launch { coachMarkState.showPreviousCoachMark() } }
    }
    val onDismissCoachMark: () -> Unit = remember {
        { scope.launch { lazyListState.animateScrollToItem(index = 0) } }
    }

    val passwordHandlers = rememberPasswordHandlers(viewModel)
    val passphraseHandlers = rememberPassphraseHandlers(viewModel)
    val usernameTypeHandlers = rememberUsernameTypeHandlers(viewModel)
    val forwardedEmailAliasHandlers = rememberForwardedEmailAliasHandlers(viewModel)
    val plusAddressedEmailHandlers = rememberPlusAddressedEmailHandlers(viewModel)
    val catchAllEmailHandlers = rememberCatchAllEmailHandlers(viewModel)
    val randomWordHandlers = rememberRandomWordHandlers(viewModel)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CoachMarkContainer(
        state = coachMarkState,
        modifier = Modifier.fillMaxSize(),
    ) {
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
                            onSaveClick = remember(viewModel) {
                                { viewModel.trySendAction(GeneratorAction.SaveClick) }
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
                    onShowNextCoachMark = onShowNextCoachMark,
                    onShowPreviousCoachMark = onShowPreviousCoachMark,
                    onDismissCoachMark = onDismissCoachMark,
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
                onShowNextCoachMark = onShowNextCoachMark,
                onShowPreviousCoachMark = onShowPreviousCoachMark,
                onDismissCoachMark = onDismissCoachMark,
                onCoachMarkComplete = {
                    coachMarkState.coachingComplete(
                        onComplete = onDismissCoachMark,
                    )
                },
                lazyListState = lazyListState,
            )
        }
    }
    // Remove dim nav bar effect when we leave this screen.
    DisposableEffect(Unit) {
        onDispose {
            onDimNavBarRequest(false)
        }
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
        title = stringResource(id = BitwardenString.generator),
        scrollBehavior = scrollBehavior,
        dividerStyle = TopAppBarDividerStyle.NONE,
        actions = {
            BitwardenOverflowActionItem(
                contentDescription = stringResource(BitwardenString.more),
                menuItemDataList = persistentListOf(
                    OverflowMenuItemData(
                        text = stringResource(id = BitwardenString.password_history),
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
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTopAppBar(
        title = stringResource(id = BitwardenString.generator),
        navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
        navigationIconContentDescription = stringResource(id = BitwardenString.close),
        onNavigationIconClick = onCloseClick,
        scrollBehavior = scrollBehavior,
        dividerStyle = when (generatorMode) {
            GeneratorMode.Modal.Password -> TopAppBarDividerStyle.NONE
            is GeneratorMode.Modal.Username -> TopAppBarDividerStyle.ON_SCROLL
        },
        actions = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.apply),
                onClick = onSaveClick,
                modifier = Modifier.testTag("SaveButton"),
            )
        },
        modifier = modifier,
    )
}

//endregion Top App Bar Composables

//region ScrollContent and Static Items

@Suppress("LongMethod")
@Composable
private fun CoachMarkScope<ExploreGeneratorCoachMark>.ScrollContent(
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
    lazyListState: LazyListState,
    onShowNextCoachMark: () -> Unit,
    onShowPreviousCoachMark: () -> Unit,
    onDismissCoachMark: () -> Unit,
    onCoachMarkComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxHeight(),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isUnderPolicy) {
            item {
                BitwardenInfoCalloutCard(
                    text = stringResource(id = BitwardenString.password_generator_policy_in_effect),
                    modifier = Modifier
                        .testTag("PasswordGeneratorPolicyInEffectLabel")
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (state.shouldShowExploreGeneratorCard) {
            item {
                @Suppress("MaxLineLength")
                BitwardenActionCard(
                    cardTitle = stringResource(BitwardenString.explore_the_generator),
                    cardSubtitle = stringResource(
                        BitwardenString.learn_more_about_generating_secure_login_credentials_with_guided_tour,
                    ),
                    actionText = stringResource(BitwardenString.get_started),
                    onActionClick = passwordHandlers.onGeneratorActionCardClicked,
                    onDismissClick = passwordHandlers.onGeneratorActionCardDismissed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item(key = ExploreGeneratorCoachMark.GENERATE_BUTTON) {
            GeneratedStringItem(
                generatedText = state.generatedText,
                onRegenerateClick = onRegenerateClick,
                onShowPreviousCoachMark = onShowPreviousCoachMark,
                onDismissCoachMark = onDismissCoachMark,
                onShowNextCoachMark = onShowNextCoachMark,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        @Suppress("MaxLineLength")
        coachMarkHighlightItem(
            key = ExploreGeneratorCoachMark.COPY_PASSWORD_BUTTON,
            title = BitwardenString.coachmark_6_of_6.asText(),
            description = BitwardenString
                .after_you_save_your_new_password_to_bitwarden_don_t_forget_to_update_it_on_your_account_website
                .asText(),
            shape = CoachMarkHighlightShape.RoundedRectangle(radius = 50f),
            onDismiss = onDismissCoachMark,
            leftAction = {
                CoachMarkActionText(
                    actionLabel = stringResource(BitwardenString.back),
                    onActionClick = onShowPreviousCoachMark,
                )
            },
            rightAction = {
                CoachMarkActionText(
                    actionLabel = stringResource(BitwardenString.done_text),
                    onActionClick = onCoachMarkComplete,
                )
            },
            modifier = Modifier.standardHorizontalMargin(windowAdaptiveInfo = windowAdaptiveInfo),
        ) {
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.copy),
                onClick = onCopyClick,
                modifier = Modifier
                    .testTag(tag = "CopyValueButton")
                    .fillMaxWidth(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        when (val selectedType = state.selectedType) {
            is GeneratorState.MainType.Passphrase -> {
                item {
                    PassphraseTypeContent(
                        passphraseTypeState = selectedType,
                        passphraseHandlers = passphraseHandlers,
                    )
                }
            }

            is GeneratorState.MainType.Password -> {
                coachMarkHighlightItem(
                    key = ExploreGeneratorCoachMark.PASSWORD_OPTIONS,
                    title = BitwardenString.coachmark_4_of_6.asText(),
                    description = BitwardenString
                        .use_these_options_to_adjust_your_password_to_your_account_requirements
                        .asText(),
                    onDismiss = onDismissCoachMark,
                    leftAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.back),
                            onActionClick = onShowPreviousCoachMark,
                        )
                    },
                    rightAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.next),
                            onActionClick = onShowNextCoachMark,
                        )
                    },
                    modifier = Modifier
                        .standardHorizontalMargin(windowAdaptiveInfo = windowAdaptiveInfo),
                ) {
                    PasswordTypeContent(
                        passwordTypeState = selectedType,
                        passwordHandlers = passwordHandlers,
                    )
                }
            }

            is GeneratorState.MainType.Username -> {
                item {
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

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun CoachMarkScope<ExploreGeneratorCoachMark>.GeneratedStringItem(
    generatedText: String,
    onRegenerateClick: () -> Unit,
    onShowPreviousCoachMark: () -> Unit,
    onDismissCoachMark: () -> Unit,
    onShowNextCoachMark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = null,
        textFieldTestTag = "GeneratedPasswordLabel",
        value = generatedText,
        singleLine = false,
        actions = {
            CoachMarkHighlight(
                key = ExploreGeneratorCoachMark.GENERATE_BUTTON,
                title = stringResource(BitwardenString.coachmark_5_of_6),
                description = stringResource(
                    BitwardenString.use_this_button_to_generate_a_new_unique_password,
                ),
                shape = CoachMarkHighlightShape.Oval,
                onDismiss = onDismissCoachMark,
                leftAction = {
                    CoachMarkActionText(
                        actionLabel = stringResource(BitwardenString.back),
                        onActionClick = onShowPreviousCoachMark,
                    )
                },
                rightAction = {
                    CoachMarkActionText(
                        actionLabel = stringResource(BitwardenString.next),
                        onActionClick = onShowNextCoachMark,
                    )
                },
            ) {
                BitwardenStandardIconButton(
                    vectorIconRes = BitwardenDrawable.ic_generate,
                    contentDescription = stringResource(id = BitwardenString.generate_password),
                    onClick = onRegenerateClick,
                    modifier = Modifier.testTag("RegenerateValueButton"),
                )
            }
        },
        onValueChange = {},
        readOnly = true,
        textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
        shouldAddCustomLineBreaks = true,
        visualTransformation = nonLetterColorVisualTransformation(),
        modifier = modifier,
        textToolbarType = TextToolbarType.NONE,
        cardStyle = CardStyle.Full,
    )
}

@Suppress("MaxLineLength", "LongMethod")
@Composable
private fun CoachMarkScope<ExploreGeneratorCoachMark>.MainStateOptionsItem(
    selectedType: GeneratorState.MainType,
    passcodePolicyOverride: GeneratorState.PasscodePolicyOverride?,
    possibleMainStates: ImmutableList<GeneratorState.MainTypeOption>,
    onMainStateOptionClicked: (GeneratorState.MainTypeOption) -> Unit,
    onShowNextCoachMark: () -> Unit,
    onShowPreviousCoachMark: () -> Unit,
    onDismissCoachMark: () -> Unit,
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
    ) { index, weightedWidth, option ->
        when (index) {
            0 -> {
                CoachMarkHighlight(
                    key = ExploreGeneratorCoachMark.PASSWORD_MODE,
                    title = stringResource(BitwardenString.coachmark_1_of_6),
                    description = stringResource(
                        BitwardenString.use_the_generator_to_create_secure_passwords_passphrases_and_usernames,
                    ),
                    onDismiss = onDismissCoachMark,
                    rightAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.next),
                            onActionClick = onShowNextCoachMark,
                        )
                    },
                    shape = CoachMarkHighlightShape.RoundedRectangle(radius = 50f),
                    leftAction = null,
                ) {
                    SegmentedButtonOptionContent(
                        option = option,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(weightedWidth),
                    )
                }
            }

            1 -> {
                CoachMarkHighlight(
                    key = ExploreGeneratorCoachMark.PASSPHRASE_MODE,
                    title = stringResource(BitwardenString.coachmark_2_of_6),
                    description = stringResource(
                        BitwardenString.passphrases_are_strong_passwords_that_are_often_easier_to_remember_and_type_than_random_passwords,
                    ),
                    onDismiss = onDismissCoachMark,
                    rightAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.next),
                            onActionClick = onShowNextCoachMark,
                        )
                    },
                    leftAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.back),
                            onActionClick = onShowPreviousCoachMark,
                        )
                    },
                    shape = CoachMarkHighlightShape.RoundedRectangle(radius = 50f),
                ) {
                    SegmentedButtonOptionContent(
                        option = option,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(weightedWidth),
                    )
                }
            }

            2 -> {
                CoachMarkHighlight(
                    key = ExploreGeneratorCoachMark.USERNAME_MODE,
                    title = stringResource(BitwardenString.coachmark_3_of_6),
                    description = stringResource(
                        BitwardenString.unique_usernames_add_an_extra_layer_of_security_and_can_help_prevent_hackers_from_finding_your_accounts,
                    ),
                    onDismiss = onDismissCoachMark,
                    rightAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.next),
                            onActionClick = onShowNextCoachMark,
                        )
                    },
                    leftAction = {
                        CoachMarkActionText(
                            actionLabel = stringResource(BitwardenString.back),
                            onActionClick = onShowPreviousCoachMark,
                        )
                    },
                    shape = CoachMarkHighlightShape.RoundedRectangle(radius = 50f),
                ) {
                    SegmentedButtonOptionContent(
                        option = option,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(weightedWidth),
                    )
                }
            }

            else -> {
                SegmentedButtonOptionContent(
                    option = option,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(weightedWidth),
                )
            }
        }
    }
}

//endregion ScrollContent and Static Items

//region PasswordType Composables

@Suppress("LongMethod")
@Composable
private fun PasswordTypeContent(
    passwordTypeState: GeneratorState.MainType.Password,
    passwordHandlers: PasswordHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordCapitalLettersToggleItem(
            useCapitals = passwordTypeState.useCapitals,
            onPasswordToggleCapitalLettersChange = passwordHandlers
                .onPasswordToggleCapitalLettersChange,
            enabled = passwordTypeState.capitalsEnabled,
            modifier = Modifier
                .fillMaxWidth(),
        )
        PasswordLowercaseLettersToggleItem(
            useLowercase = passwordTypeState.useLowercase,
            onPasswordToggleLowercaseLettersChange = passwordHandlers
                .onPasswordToggleLowercaseLettersChange,
            enabled = passwordTypeState.lowercaseEnabled,
            modifier = Modifier
                .fillMaxWidth(),
        )
        PasswordNumbersToggleItem(
            useNumbers = passwordTypeState.useNumbers,
            onPasswordToggleNumbersChange = passwordHandlers.onPasswordToggleNumbersChange,
            enabled = passwordTypeState.numbersEnabled,
            modifier = Modifier
                .fillMaxWidth(),
        )
        PasswordSpecialCharactersToggleItem(
            useSpecialChars = passwordTypeState.useSpecialChars,
            onPasswordToggleSpecialCharactersChange = passwordHandlers
                .onPasswordToggleSpecialCharactersChange,
            enabled = passwordTypeState.specialCharsEnabled,
            modifier = Modifier
                .fillMaxWidth(),
        )
        PasswordAvoidAmbiguousCharsToggleItem(
            avoidAmbiguousChars = passwordTypeState.avoidAmbiguousChars,
            onPasswordToggleAvoidAmbiguousCharsChange = passwordHandlers
                .onPasswordToggleAvoidAmbiguousCharsChange,
            enabled = passwordTypeState.ambiguousCharsEnabled,
            modifier = Modifier
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordMinNumbersCounterItem(
            minNumbers = passwordTypeState.minNumbers,
            onPasswordMinNumbersCounterChange = passwordHandlers.onPasswordMinNumbersCounterChange,
            maxValue = max(
                passwordTypeState.maxNumbersAllowed,
                passwordTypeState.minNumbersAllowed,
            ),
            minValue = passwordTypeState.minNumbersAllowed,
            modifier = Modifier
                .fillMaxWidth(),
        )

        PasswordMinSpecialCharactersCounterItem(
            minSpecial = passwordTypeState.minSpecial,
            onPasswordMinSpecialCharactersChange = passwordHandlers
                .onPasswordMinSpecialCharactersChange,
            maxValue = max(
                passwordTypeState.maxSpecialAllowed,
                passwordTypeState.minSpecialAllowed,
            ),
            minValue = passwordTypeState.minSpecialAllowed,
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
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
        contentDescription = stringResource(id = BitwardenString.uppercase_ato_z),
        isChecked = useCapitals,
        onCheckedChange = onPasswordToggleCapitalLettersChange,
        enabled = enabled,
        cardStyle = CardStyle.Top(),
        modifier = modifier.testTag(tag = "UppercaseAtoZToggle"),
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
        contentDescription = stringResource(id = BitwardenString.lowercase_ato_z),
        isChecked = useLowercase,
        onCheckedChange = onPasswordToggleLowercaseLettersChange,
        enabled = enabled,
        cardStyle = CardStyle.Middle(),
        modifier = modifier.testTag(tag = "LowercaseAtoZToggle"),
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
        contentDescription = stringResource(id = BitwardenString.numbers_zero_to_nine),
        isChecked = useNumbers,
        onCheckedChange = onPasswordToggleNumbersChange,
        enabled = enabled,
        cardStyle = CardStyle.Middle(),
        modifier = modifier.testTag(tag = "NumbersZeroToNineToggle"),
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
        contentDescription = stringResource(id = BitwardenString.special_characters),
        isChecked = useSpecialChars,
        onCheckedChange = onPasswordToggleSpecialCharactersChange,
        enabled = enabled,
        cardStyle = CardStyle.Middle(),
        modifier = modifier.testTag(tag = "SpecialCharactersToggle"),
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
        label = stringResource(id = BitwardenString.min_numbers),
        value = minNumbers.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinNumbersCounterChange,
        cardStyle = CardStyle.Top(),
        modifier = modifier.testTag(tag = "MinNumberValueLabel"),
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
        label = stringResource(id = BitwardenString.min_special),
        value = minSpecial.coerceIn(minValue, maxValue),
        range = minValue..maxValue,
        onValueChange = onPasswordMinSpecialCharactersChange,
        cardStyle = CardStyle.Bottom,
        modifier = modifier.testTag(tag = "MinSpecialValueLabel"),
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
        label = stringResource(id = BitwardenString.avoid_ambiguous_characters),
        isChecked = avoidAmbiguousChars,
        enabled = enabled,
        onCheckedChange = onPasswordToggleAvoidAmbiguousCharsChange,
        cardStyle = CardStyle.Bottom,
        modifier = modifier.testTag(tag = "AvoidAmbiguousCharsToggle"),
    )
}

//endregion PasswordType Composables

//region PassphraseType Composables

@Composable
private fun PassphraseTypeContent(
    passphraseTypeState: GeneratorState.MainType.Passphrase,
    passphraseHandlers: PassphraseHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PassphraseNumWordsCounterItem(
            numWords = passphraseTypeState.numWords,
            onPassphraseNumWordsCounterChange = passphraseHandlers
                .onPassphraseNumWordsCounterChange,
            minValue = passphraseTypeState.minNumWords,
            maxValue = passphraseTypeState.maxNumWords,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        PassphraseWordSeparatorInputItem(
            wordSeparator = passphraseTypeState.wordSeparator,
            onPassphraseWordSeparatorChange = passphraseHandlers.onPassphraseWordSeparatorChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        PassphraseCapitalizeToggleItem(
            capitalize = passphraseTypeState.capitalize,
            onPassphraseCapitalizeToggleChange = passphraseHandlers
                .onPassphraseCapitalizeToggleChange,
            enabled = passphraseTypeState.capitalizeEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        PassphraseIncludeNumberToggleItem(
            includeNumber = passphraseTypeState.includeNumber,
            onPassphraseIncludeNumberToggleChange = passphraseHandlers
                .onPassphraseIncludeNumberToggleChange,
            enabled = passphraseTypeState.includeNumberEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
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
    BitwardenStepper(
        label = stringResource(id = BitwardenString.number_of_words),
        value = numWords.coerceIn(minimumValue = minValue, maximumValue = maxValue),
        range = minValue..maxValue,
        onValueChange = onPassphraseNumWordsCounterChange,
        cardStyle = CardStyle.Full,
        modifier = modifier.testTag(tag = "NumberOfWordsStepper"),
    )
}

@Composable
private fun PassphraseWordSeparatorInputItem(
    wordSeparator: Char?,
    onPassphraseWordSeparatorChange: (wordSeparator: Char?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = BitwardenString.word_separator),
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
        cardStyle = CardStyle.Full,
        textFieldTestTag = "WordSeparatorEntry",
        modifier = modifier,
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
        label = stringResource(id = BitwardenString.capitalize),
        isChecked = capitalize,
        onCheckedChange = onPassphraseCapitalizeToggleChange,
        enabled = enabled,
        cardStyle = CardStyle.Top(),
        modifier = modifier.testTag(tag = "CapitalizePassphraseToggle"),
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
        label = stringResource(id = BitwardenString.include_number),
        isChecked = includeNumber,
        enabled = enabled,
        onCheckedChange = onPassphraseIncludeNumberToggleChange,
        cardStyle = CardStyle.Bottom,
        modifier = modifier.testTag(tag = "IncludeNumbersToggle"),
    )
}

//endregion PassphraseType Composables

//region UsernameType Composables

@Composable
private fun UsernameTypeItems(
    usernameState: GeneratorState.MainType.Username,
    onSubStateOptionClicked: (GeneratorState.MainType.Username.UsernameTypeOption) -> Unit,
    usernameTypeHandlers: UsernameTypeHandlers,
    forwardedEmailAliasHandlers: ForwardedEmailAliasHandlers,
    plusAddressedEmailHandlers: PlusAddressedEmailHandlers,
    catchAllEmailHandlers: CatchAllEmailHandlers,
    randomWordHandlers: RandomWordHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        UsernameOptionsItem(
            currentSubState = usernameState,
            onSubStateOptionClicked = onSubStateOptionClicked,
            usernameTypeHandlers = usernameTypeHandlers,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

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
        label = stringResource(id = BitwardenString.username_type),
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
            contentDescription = stringResource(id = BitwardenString.learn_more),
        ),
        cardStyle = CardStyle.Full,
        modifier = modifier.testTag(tag = "UsernameTypePicker"),
    )
}

//endregion UsernameType Composables

//region ForwardedEmailAliasType Composables

@Suppress("LongMethod")
@Composable
private fun ForwardedEmailAliasTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias,
    forwardedEmailAliasHandlers: ForwardedEmailAliasHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        ServiceTypeOptionsItem(
            currentSubState = usernameTypeState,
            onSubStateOptionClicked = forwardedEmailAliasHandlers.onServiceChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (usernameTypeState.selectedServiceType) {
            is ServiceType.AddyIo -> {
                BitwardenPasswordField(
                    label = stringResource(
                        id = BitwardenString.api_access_token_required_parenthesis,
                    ),
                    value = usernameTypeState.selectedServiceType.apiAccessToken,
                    onValueChange = forwardedEmailAliasHandlers.onAddyIoAccessTokenTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                BitwardenTextField(
                    label = stringResource(id = BitwardenString.domain_name_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.domainName,
                    onValueChange = forwardedEmailAliasHandlers.onAddyIoDomainNameTextChange,
                    textFieldTestTag = "AnonAddyDomainNameEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                BitwardenTextField(
                    label = stringResource(id = BitwardenString.self_host_server_url),
                    value = usernameTypeState.selectedServiceType.selfHostServerUrl,
                    onValueChange = forwardedEmailAliasHandlers.onAddyIoSelfHostServerUrlChange,
                    textFieldTestTag = "AnonAddySelfHostUrlEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            is ServiceType.DuckDuckGo -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.api_key_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.apiKey,
                    onValueChange = forwardedEmailAliasHandlers.onDuckDuckGoApiKeyTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            is ServiceType.FastMail -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.api_key_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.apiKey,
                    onValueChange = forwardedEmailAliasHandlers.onFastMailApiKeyTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            is ServiceType.FirefoxRelay -> {
                BitwardenPasswordField(
                    label = stringResource(
                        id = BitwardenString.api_access_token_required_parenthesis,
                    ),
                    value = usernameTypeState.selectedServiceType.apiAccessToken,
                    onValueChange = forwardedEmailAliasHandlers.onFirefoxRelayAccessTokenTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            is ServiceType.ForwardEmail -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.api_key_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.apiKey,
                    onValueChange = forwardedEmailAliasHandlers.onForwardEmailApiKeyTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                BitwardenTextField(
                    label = stringResource(id = BitwardenString.domain_name_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.domainName,
                    onValueChange = forwardedEmailAliasHandlers.onForwardEmailDomainNameTextChange,
                    textFieldTestTag = "ForwardedEmailDomainNameEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            is ServiceType.SimpleLogin -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.api_key_required_parenthesis),
                    value = usernameTypeState.selectedServiceType.apiKey,
                    onValueChange = forwardedEmailAliasHandlers.onSimpleLoginApiKeyTextChange,
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                BitwardenTextField(
                    label = stringResource(id = BitwardenString.self_host_server_url),
                    value = usernameTypeState.selectedServiceType.selfHostServerUrl,
                    onValueChange = forwardedEmailAliasHandlers
                        .onSimpleLoginSelfHostServerUrlChange,
                    textFieldTestTag = "SimpleLoginSelfHostServerUrlEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }

            null -> {
                var obfuscatedTextField by remember { mutableStateOf("") }
                BitwardenPasswordField(
                    label = null,
                    value = obfuscatedTextField,
                    onValueChange = { obfuscatedTextField = it },
                    showPasswordTestTag = "ShowForwardedEmailApiSecretButton",
                    passwordFieldTestTag = "ForwardedEmailApiSecretEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
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
        label = stringResource(id = BitwardenString.service),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = (currentSubState.selectedServiceType?.displayStringResId)?.let {
            stringResource(id = it)
        },
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onSubStateOptionClicked(selectedOptionId)
        },
        cardStyle = CardStyle.Full,
        modifier = modifier.testTag(tag = "ServiceTypePicker"),
    )
}

//region PlusAddressedEmailType Composables

@Composable
private fun PlusAddressedEmailTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.PlusAddressedEmail,
    plusAddressedEmailHandlers: PlusAddressedEmailHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        PlusAddressedEmailTextInputItem(
            email = usernameTypeState.email,
            onPlusAddressedEmailTextChange = plusAddressedEmailHandlers.onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun PlusAddressedEmailTextInputItem(
    email: String,
    onPlusAddressedEmailTextChange: (email: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = BitwardenString.email_required_parenthesis),
        value = email,
        onValueChange = onPlusAddressedEmailTextChange,
        textFieldTestTag = "PlusAddressedEmailEntry",
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

//endregion PlusAddressedEmailType Composables

//region CatchAllEmailType Composables

@Composable
private fun CatchAllEmailTypeContent(
    usernameTypeState: GeneratorState.MainType.Username.UsernameType.CatchAllEmail,
    catchAllEmailHandlers: CatchAllEmailHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        CatchAllEmailTextInputItem(
            domain = usernameTypeState.domainName,
            onDomainTextChange = catchAllEmailHandlers.onDomainChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun CatchAllEmailTextInputItem(
    domain: String,
    onDomainTextChange: (domain: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = BitwardenString.domain_name_required_parenthesis),
        value = domain,
        onValueChange = onDomainTextChange,
        textFieldTestTag = "CatchAllEmailDomainEntry",
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

//endregion CatchAllEmailType Composables

//region Random Word Composables

@Composable
private fun RandomWordTypeContent(
    randomWordTypeState: GeneratorState.MainType.Username.UsernameType.RandomWord,
    randomWordHandlers: RandomWordHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        RandomWordCapitalizeToggleItem(
            capitalize = randomWordTypeState.capitalize,
            onRandomWordCapitalizeToggleChange = randomWordHandlers.onCapitalizeChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        RandomWordIncludeNumberToggleItem(
            includeNumber = randomWordTypeState.includeNumber,
            onRandomWordIncludeNumberToggleChange = randomWordHandlers.onIncludeNumberChange,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun RandomWordCapitalizeToggleItem(
    capitalize: Boolean,
    onRandomWordCapitalizeToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = stringResource(id = BitwardenString.capitalize),
        isChecked = capitalize,
        onCheckedChange = onRandomWordCapitalizeToggleChange,
        cardStyle = CardStyle.Top(),
        modifier = modifier.testTag(tag = "CapitalizeRandomWordUsernameToggle"),
    )
}

@Composable
private fun RandomWordIncludeNumberToggleItem(
    includeNumber: Boolean,
    onRandomWordIncludeNumberToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenSwitch(
        label = stringResource(id = BitwardenString.include_number),
        isChecked = includeNumber,
        onCheckedChange = onRandomWordIncludeNumberToggleChange,
        cardStyle = CardStyle.Bottom,
        modifier = modifier.testTag(tag = "IncludeNumberRandomWordUsernameToggle"),
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
            onDimNavBarRequest = {},
        )
    }
}
