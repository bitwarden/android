package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.content.res.Resources
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.chrome.ChromeAutofillSettingsCard
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.displayLabel
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
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
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

            is AutoFillEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }

            AutoFillEvent.NavigateToBlockAutoFill -> {
                onNavigateToBlockAutoFillScreen()
            }

            AutoFillEvent.NavigateToSettings -> {
                intentManager.startCredentialManagerSettings(context)
            }

            AutoFillEvent.NavigateToSetupAutofill -> onNavigateToSetupAutofill()
            is AutoFillEvent.NavigateToChromeAutofillSettings -> {
                intentManager.startChromeAutofillSettingsActivity(
                    releaseChannel = event.releaseChannel,
                )
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
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.autofill),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.BackClick) }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            AnimatedVisibility(
                visible = state.showAutofillActionCard,
                label = "AutofillActionCard",
                exit = actionCardExitAnimation(),
            ) {
                BitwardenActionCard(
                    cardTitle = stringResource(R.string.turn_on_autofill),
                    actionText = stringResource(R.string.get_started),
                    onActionClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(AutoFillAction.AutofillActionCardCtaClick)
                        }
                    },
                    onDismissClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(AutoFillAction.DismissShowAutofillActionCard)
                        }
                    },
                    leadingContent = {
                        NotificationBadge(notificationCount = 1)
                    },
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
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(it)) }
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AutofillServicesSwitch")
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            if (state.showInlineAutofillOption) {
                BitwardenSwitch(
                    label = stringResource(id = R.string.inline_autofill),
                    supportingText = stringResource(
                        id = R.string.use_inline_autofill_explanation_long,
                    ),
                    isChecked = state.isUseInlineAutoFillEnabled,
                    onCheckedChange = remember(viewModel) {
                        { viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(it)) }
                    },
                    enabled = state.canInteractWithInlineAutofillToggle,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("InlineAutofillSwitch")
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            if (state.chromeAutofillSettingsOptions.isNotEmpty()) {
                ChromeAutofillSettingsCard(
                    options = state.chromeAutofillSettingsOptions,
                    onOptionClicked = remember(viewModel) {
                        {
                            viewModel.trySendAction(AutoFillAction.ChromeAutofillSelected(it))
                        }
                    },
                    enabled = state.isAutoFillServicesEnabled,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.showPasskeyManagementRow) {
                BitwardenExternalLinkRow(
                    text = stringResource(id = R.string.passkey_management),
                    description = stringResource(
                        id = R.string.passkey_management_explanation_long,
                    ),
                    onConfirmClick = remember(viewModel) {
                        { viewModel.trySendAction(AutoFillAction.PasskeyManagementClick) }
                    },
                    dialogTitle = stringResource(id = R.string.continue_to_device_settings),
                    dialogMessage = stringResource(
                        id = R.string.set_bitwarden_as_passkey_manager_description,
                    ),
                    withDivider = false,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            AccessibilityAutofillSwitch(
                isAccessibilityAutoFillEnabled = state.isAccessibilityAutofillEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick) }
                },
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
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(it)) }
                },
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
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(it)) }
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AskToAddLoginSwitch")
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            DefaultUriMatchTypeRow(
                selectedUriMatchType = state.defaultUriMatchType,
                onUriMatchTypeSelect = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.DefaultUriMatchTypeSelect(it)) }
                },
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
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) }
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AccessibilityAutofillSwitch(
    isAccessibilityAutoFillEnabled: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    BitwardenSwitch(
        label = stringResource(id = R.string.accessibility),
        supportingText = stringResource(id = R.string.accessibility_description5),
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
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.default_uri_match_detection),
        options = UriMatchType.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = selectedUriMatchType.displayLabel(),
        onOptionSelected = { selectedOption ->
            onUriMatchTypeSelect(
                UriMatchType
                    .entries
                    .first { it.displayLabel.toString(resources) == selectedOption },
            )
        },
        supportingText = stringResource(id = R.string.default_uri_match_detection_description),
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
