package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch

/**
 * Displays the auto-fill screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFillScreen(
    onNavigateBack: () -> Unit,
    viewModel: AutoFillViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AutoFillEvent.NavigateBack -> onNavigateBack.invoke()

            is AutoFillEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }
        }
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
                navigationIcon = painterResource(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.BackClick) }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.autofill),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenWideSwitch(
                label = stringResource(id = R.string.autofill_services),
                description = stringResource(id = R.string.autofill_services_explanation_long),
                isChecked = state.isAutoFillServicesEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenWideSwitch(
                label = stringResource(id = R.string.inline_autofill),
                description = stringResource(id = R.string.use_inline_autofill_explanation_long),
                isChecked = state.isUseInlineAutoFillEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenWideSwitch(
                label = stringResource(id = R.string.accessibility),
                description = stringResource(id = R.string.accessibility_description4),
                isChecked = state.isUseAccessibilityEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UseAccessibilityClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenWideSwitch(
                label = stringResource(id = R.string.draw_over),
                description = stringResource(id = R.string.draw_over_description3),
                isChecked = state.isUseDrawOverEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UseDrawOverClick(it)) }
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
            BitwardenWideSwitch(
                label = stringResource(id = R.string.copy_totp_automatically),
                description = stringResource(id = R.string.copy_totp_automatically_description),
                isChecked = state.isCopyTotpAutomaticallyEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            BitwardenWideSwitch(
                label = stringResource(id = R.string.ask_to_add_login),
                description = stringResource(id = R.string.ask_to_add_login_description),
                isChecked = state.isAskToAddLoginEnabled,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            UriMatchDetectionDialog(
                selectedUriDetection = state.uriDetectionMethod,
                onDetectionSelect = remember(viewModel) {
                    { viewModel.trySendAction(AutoFillAction.UriDetectionMethodSelect(it)) }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UriMatchDetectionDialog(
    selectedUriDetection: AutoFillState.UriDetectionMethod,
    onDetectionSelect: (AutoFillState.UriDetectionMethod) -> Unit,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextRow(
        text = stringResource(id = R.string.default_uri_match_detection),
        description = stringResource(id = R.string.default_uri_match_detection_description),
        onClick = { shouldShowDialog = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = selectedUriDetection.text(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.default_uri_match_detection),
            onDismissRequest = { shouldShowDialog = false },
        ) {
            AutoFillState.UriDetectionMethod.values().forEach { option ->
                BitwardenSelectionRow(
                    text = option.text,
                    isSelected = option == selectedUriDetection,
                    onClick = {
                        shouldShowDialog = false
                        onDetectionSelect(
                            AutoFillState.UriDetectionMethod.values().first { it == option },
                        )
                    },
                )
            }
        }
    }
}
