package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.badge.NotificationBadge
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.x8bit.bitwarden.ui.platform.components.card.actionCardExitAnimation
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenExternalLinkRow
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util.displayLabel
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
                            viewModel.trySendAction(AutoFillAction.AutoFillActionCardCtaClick)
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
                        .padding(top = 12.dp, bottom = 16.dp),
                )
            }
            BitwardenListHeaderText(
                label = stringResource(id = R.string.autofill),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenSwitch(
                label = stringResource(id = R.string.autofill_services),
                description = stringResource(id = R.string.autofill_services_explanation_long),
                isChecked = state.isAutoFillServicesEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AutofillServicesSwitch")
                    .padding(horizontal = 16.dp),
            )
            if (state.showInlineAutofillOption) {
                BitwardenSwitch(
                    label = stringResource(id = R.string.inline_autofill),
                    description = stringResource(
                        id = R.string.use_inline_autofill_explanation_long,
                    ),
                    isChecked = state.isUseInlineAutoFillEnabled,
                    onCheckedChange = remember(viewModel) {
                        { viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(it)) }
                    },
                    enabled = state.canInteractWithInlineAutofillToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("InlineAutofillSwitch")
                        .padding(horizontal = 16.dp),
                )
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
                )
            }
            AccessibilityAutofillSwitch(
                isAccessibilityAutoFillEnabled = state.isAccessibilityAutofillEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.additional_options),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenSwitch(
                label = stringResource(id = R.string.copy_totp_automatically),
                description = stringResource(id = R.string.copy_totp_automatically_description),
                isChecked = state.isCopyTotpAutomaticallyEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("CopyTotpAutomaticallySwitch")
                    .padding(horizontal = 16.dp),
            )
            BitwardenSwitch(
                label = stringResource(id = R.string.ask_to_add_login),
                description = stringResource(id = R.string.ask_to_add_login_description),
                isChecked = state.isAskToAddLoginEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AskToAddLoginSwitch")
                    .padding(horizontal = 16.dp),
            )
            DefaultUriMatchTypeRow(
                selectedUriMatchType = state.defaultUriMatchType,
                onUriMatchTypeSelect = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.DefaultUriMatchTypeSelect(it)) }
                },
                modifier = Modifier
                    .testTag("DefaultUriMatchDetectionChooser")
                    .fillMaxWidth(),
            )
            BitwardenTextRow(
                text = stringResource(id = R.string.block_auto_fill),
                description = stringResource(
                    id = R.string.auto_fill_will_not_be_offered_for_these_ur_is,
                ),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
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
        description = stringResource(id = R.string.accessibility_description5),
        isChecked = isAccessibilityAutoFillEnabled,
        onCheckedChange = {
            if (isAccessibilityAutoFillEnabled) {
                onCheckedChange()
            } else {
                shouldShowDialog = true
            }
        },
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
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextRow(
        text = stringResource(id = R.string.default_uri_match_detection),
        description = stringResource(id = R.string.default_uri_match_detection_description),
        onClick = { shouldShowDialog = true },
        modifier = modifier,
    ) {
        Text(
            text = selectedUriMatchType.displayLabel(),
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.default_uri_match_detection),
            onDismissRequest = { shouldShowDialog = false },
        ) {
            val uriMatchTypes = UriMatchType.entries
            uriMatchTypes.forEach { option ->
                BitwardenSelectionRow(
                    text = option.displayLabel,
                    isSelected = option == selectedUriMatchType,
                    onClick = {
                        shouldShowDialog = false
                        onUriMatchTypeSelect(
                            uriMatchTypes.first { it == option },
                        )
                    },
                )
            }
        }
    }
}
