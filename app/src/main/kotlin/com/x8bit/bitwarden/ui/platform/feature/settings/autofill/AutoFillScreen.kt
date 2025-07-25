package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.BrowserAutofillSettingsCard
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.handlers.AutoFillHandlers
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.displayLabel
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.isAdvancedMatching
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the auto-fill screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFillScreen(
    onNavigateBack: () -> Unit,
    viewModel: AutoFillViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    onNavigateToBlockAutoFillScreen: () -> Unit,
    onNavigateToSetupAutofill: () -> Unit,
    onNavigateToAboutPrivilegedAppsScreen: () -> Unit,
    onNavigateToPrivilegedAppsList: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var shouldShowAutofillFallbackDialog by rememberSaveable { mutableStateOf(false) }
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AutoFillEvent.NavigateBack -> onNavigateBack.invoke()

            AutoFillEvent.NavigateToAccessibilitySettings -> {
                intentManager.startSystemAccessibilitySettingsActivity()
            }

            AutoFillEvent.NavigateToAutofillSettings -> {
                val isSuccess = intentManager.startSystemAutofillSettingsActivity()

                shouldShowAutofillFallbackDialog = !isSuccess
            }

            AutoFillEvent.NavigateToBlockAutoFill -> {
                onNavigateToBlockAutoFillScreen()
            }

            AutoFillEvent.NavigateToSettings -> {
                intentManager.startCredentialManagerSettings(context)
            }

            AutoFillEvent.NavigateToSetupAutofill -> onNavigateToSetupAutofill()
            is AutoFillEvent.NavigateToBrowserAutofillSettings -> {
                intentManager.startBrowserAutofillSettingsActivity(
                    browserPackage = event.browserPackage,
                )
            }

            AutoFillEvent.NavigateToAboutPrivilegedAppsScreen -> {
                onNavigateToAboutPrivilegedAppsScreen()
            }

            AutoFillEvent.NavigateToPrivilegedAppsListScreen -> {
                onNavigateToPrivilegedAppsList()
            }

            AutoFillEvent.NavigateToMatchDetectionInfo -> {
                intentManager.launchUri("https://bitwarden.com/help/uri-match-detection/".toUri())
            }
        }
    }

    if (shouldShowAutofillFallbackDialog) {
        BitwardenBasicDialog(
            title = null,
            message = stringResource(id = R.string.bitwarden_autofill_go_to_settings),
            onDismissRequest = { shouldShowAutofillFallbackDialog = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val autoFillHandlers = remember(viewModel) { AutoFillHandlers.create(viewModel = viewModel) }
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.autofill),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.BackClick) }
                },
            )
        },
    ) {
        AutoFillScreenContent(
            state = state,
            autoFillHandlers = autoFillHandlers,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun AutoFillScreenContent(
    state: AutoFillState,
    autoFillHandlers: AutoFillHandlers,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        AnimatedVisibility(
            visible = state.showAutofillActionCard,
            label = "AutofillActionCard",
            exit = actionCardExitAnimation(),
        ) {
            BitwardenActionCard(
                cardTitle = stringResource(R.string.turn_on_autofill),
                actionText = stringResource(R.string.get_started),
                onActionClick = autoFillHandlers.onAutofillActionCardClick,
                onDismissClick = autoFillHandlers.onAutofillActionCardDismissClick,
                leadingContent = { NotificationBadge(notificationCount = 1) },
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(bottom = 16.dp),
            )
        }
        BitwardenListHeaderText(
            label = stringResource(id = R.string.autofill),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.autofill_services),
            supportingText = stringResource(id = R.string.autofill_services_explanation_long),
            isChecked = state.isAutoFillServicesEnabled,
            onCheckedChange = autoFillHandlers.onAutofillServicesClick,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("AutofillServicesSwitch")
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))

        AnimatedVisibility(visible = state.showInlineAutofill) {
            Column {
                FillStyleSelector(
                    selectedStyle = state.autofillStyle,
                    onStyleChange = autoFillHandlers.onAutofillStyleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
        }

        AnimatedVisibility(visible = state.showBrowserSettingOptions) {
            Column {
                BrowserAutofillSettingsCard(
                    options = state.browserAutofillSettingsOptions,
                    onOptionClicked = autoFillHandlers.onBrowserAutofillSelected,
                    enabled = state.isAutoFillServicesEnabled,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (state.showPasskeyManagementRow) {
            BitwardenExternalLinkRow(
                text = stringResource(id = R.string.passkey_management),
                description = stringResource(
                    id = R.string.passkey_management_explanation_long,
                ),
                onConfirmClick = autoFillHandlers.onPasskeyManagementClick,
                dialogTitle = stringResource(id = R.string.continue_to_device_settings),
                dialogMessage = stringResource(
                    id = R.string.set_bitwarden_as_passkey_manager_description,
                ),
                withDivider = false,
                cardStyle = if (state.isUserManagedPrivilegedAppsEnabled) {
                    CardStyle.Top()
                } else {
                    CardStyle.Full
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            if (state.isUserManagedPrivilegedAppsEnabled) {
                BitwardenTextRow(
                    text = stringResource(R.string.privileged_apps),
                    onClick = autoFillHandlers.onPrivilegedAppsClick,
                    tooltip = TooltipData(
                        contentDescription =
                            stringResource(R.string.learn_more_about_privileged_apps),
                        onClick = autoFillHandlers.onPrivilegedAppsHelpLinkClick,
                    ),
                    cardStyle = CardStyle.Bottom,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
        AccessibilityAutofillSwitch(
            isAccessibilityAutoFillEnabled = state.isAccessibilityAutofillEnabled,
            onCheckedChange = autoFillHandlers.onUseAccessibilityServiceClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.additional_options),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.copy_totp_automatically),
            supportingText = stringResource(id = R.string.copy_totp_automatically_description),
            isChecked = state.isCopyTotpAutomaticallyEnabled,
            onCheckedChange = autoFillHandlers.onCopyTotpAutomaticallyClick,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("CopyTotpAutomaticallySwitch")
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenSwitch(
            label = stringResource(id = R.string.ask_to_add_login),
            supportingText = stringResource(id = R.string.ask_to_add_login_description),
            isChecked = state.isAskToAddLoginEnabled,
            onCheckedChange = autoFillHandlers.onAskToAddLoginClick,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("AskToAddLoginSwitch")
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DefaultUriMatchTypeRow(
            selectedUriMatchType = state.defaultUriMatchType,
            onUriMatchTypeSelect = autoFillHandlers.onDefaultUriMatchTypeSelect,
            onMoreAboutMatchDetectionClick = autoFillHandlers.onMoreAboutMatchDetectionClick,
            modifier = Modifier
                .testTag("DefaultUriMatchDetectionChooser")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextRow(
            text = stringResource(id = R.string.block_auto_fill),
            description = stringResource(
                id = R.string.auto_fill_will_not_be_offered_for_these_ur_is,
            ),
            onClick = autoFillHandlers.onBlockAutoFillClick,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FillStyleSelector(
    selectedStyle: AutofillStyle,
    onStyleChange: (AutofillStyle) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.display_autofill_suggestions),
        supportingText = annotatedStringResource(
            id = BitwardenString.use_inline_autofill_explanation_long,
        ),
        options = AutofillStyle.entries.map { it.label() }.toImmutableList(),
        selectedOption = selectedStyle.label(),
        onOptionSelected = {
            onStyleChange(AutofillStyle.entries.first { style -> style.label(resources) == it })
        },
        cardStyle = CardStyle.Full,
        modifier = modifier.testTag(tag = "InlineAutofillSelector"),
    )
}

@Composable
private fun AccessibilityAutofillSwitch(
    isAccessibilityAutoFillEnabled: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    BitwardenSwitch(
        label = stringResource(id = BitwardenString.accessibility),
        supportingText = stringResource(id = BitwardenString.accessibility_description5),
        isChecked = isAccessibilityAutoFillEnabled,
        onCheckedChange = {
            if (isAccessibilityAutoFillEnabled) {
                onCheckedChange()
            } else {
                shouldShowDialog = true
            }
        },
        cardStyle = CardStyle.Full,
        modifier = modifier.testTag(tag = "AccessibilityAutofillSwitch"),
    )

    if (shouldShowDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.accessibility_service_disclosure),
            message = stringResource(id = R.string.accessibility_disclosure_text),
            confirmButtonText = stringResource(id = R.string.accept),
            dismissButtonText = stringResource(id = R.string.decline),
            onConfirmClick = {
                onCheckedChange()
                shouldShowDialog = false
            },
            onDismissClick = { shouldShowDialog = false },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

@Composable
private fun DefaultUriMatchTypeRow(
    selectedUriMatchType: UriMatchType,
    onUriMatchTypeSelect: (UriMatchType) -> Unit,
    onMoreAboutMatchDetectionClick: () -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    var showAdvancedDialog by rememberSaveable { mutableStateOf(false) }
    var optionPendingConfirmation by rememberSaveable { mutableStateOf<UriMatchType?>(null) }

    UriMatchSelectionButton(
        selectedUriMatchType = selectedUriMatchType,
        onOptionSelected = { selectedOption ->
            val newSelectedType =
                UriMatchType
                    .entries
                    .first { it.displayLabel.toString(resources) == selectedOption }

            if (newSelectedType.isAdvancedMatching()) {
                optionPendingConfirmation = newSelectedType
                showAdvancedDialog = true
            } else {
                onUriMatchTypeSelect(newSelectedType)
                optionPendingConfirmation = null
                showAdvancedDialog = false
            }
        },
        modifier = modifier,
    )

    val currentOptionToConfirm = optionPendingConfirmation
    if (showAdvancedDialog && currentOptionToConfirm != null) {
        BuildAdvancedMatchDetectionWarning(
            pendingOption = currentOptionToConfirm,
            onDialogConfirm = {
                onUriMatchTypeSelect(currentOptionToConfirm)
                showAdvancedDialog = false
                optionPendingConfirmation = null
            },
            onDialogDismiss = {
                showAdvancedDialog = false
                optionPendingConfirmation = null
            },
            onMoreAboutMatchDetectionClick = onMoreAboutMatchDetectionClick,
        )
    }
}

@Composable
private fun BuildAdvancedMatchDetectionWarning(
    pendingOption: UriMatchType,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onMoreAboutMatchDetectionClick: () -> Unit,
    resources: Resources = LocalContext.current.resources,
) {
    val moreAboutMatchDetectionStr = stringResource(R.string.more_about_match_detection)

    BitwardenTwoButtonDialog(
        titleAnnotatedString = stringResource(id = R.string.warning).toAnnotatedString(),
        messageAnnotatedString = annotatedStringResource(
            id = R.string.advanced_options_warning,
            args = arrayOf(pendingOption.displayLabel.toString(resources)),
            style = spanStyleOf(
                color = BitwardenTheme.colorScheme.text.primary,
                textStyle = BitwardenTheme.typography.bodyMedium,
            ),
        ).plus(
            AnnotatedString("\n")
                .plus(
                    annotatedStringResource(
                        id = R.string.more_about_match_detection,
                        onAnnotationClick = { annotationValue ->
                            when (annotationValue) {
                                "moreAboutMatchDetection" -> onMoreAboutMatchDetectionClick()
                            }
                        },
                    ),
                ),
        ),
        confirmButtonText = stringResource(id = R.string.continue_text),
        dismissButtonText = stringResource(id = R.string.cancel),
        onConfirmClick = onDialogConfirm,
        onDismissClick = onDialogDismiss,
        onDismissRequest = onDialogDismiss,
        messageModifier = Modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = moreAboutMatchDetectionStr,
                        action = {
                            onMoreAboutMatchDetectionClick()
                            true
                        },
                    ),
                )
            },
    )
}

@Composable
private fun UriMatchSelectionButton(
    selectedUriMatchType: UriMatchType,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    var supportingAnnotatedString =
        annotatedStringResource(id = R.string.default_uri_match_detection_description)

    if (selectedUriMatchType.isAdvancedMatching()) {
        supportingAnnotatedString = supportingAnnotatedString
            .plus(
                AnnotatedString("\n")
                    .plus(
                        annotatedStringResource(
                            id = R.string.advanced_options_warning,
                            args = arrayOf(selectedUriMatchType.displayLabel.toString(resources)),
                            style = spanStyleOf(
                                textStyle = BitwardenTheme.typography.bodySmall,
                                color = BitwardenTheme.colorScheme.text.secondary,
                            ),
                        ),
                    ),
            )
    }

    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.default_uri_match_detection),
        options = UriMatchType.entries.filter { !it.isAdvancedMatching() }
            .map { it.displayLabel() }.toImmutableList(),
        selectedOption = selectedUriMatchType.displayLabel(),
        sectionTitle = stringResource(id = R.string.advanced_options),
        sectionOptions = UriMatchType.entries.filter { it.isAdvancedMatching() }
            .map { it.displayLabel() }.toImmutableList(),
        sectionTestTag = "AdvancedOptionsSection",
        onOptionSelected = onOptionSelected,
        supportingText = supportingAnnotatedString,
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
