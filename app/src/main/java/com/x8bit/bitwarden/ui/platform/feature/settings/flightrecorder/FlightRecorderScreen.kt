package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenHyperTextLink
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util.displayText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the flight recorder configuration screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightRecorderScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlightRecorderViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel) { event ->
        when (event) {
            FlightRecorderEvent.NavigateBack -> onNavigateBack()
            FlightRecorderEvent.NavigateToHelpCenter -> {
                intentManager.launchUri(uri = "https://bitwarden.com/help/flight-recorder".toUri())
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.enable_flight_recorder_title),
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(FlightRecorderAction.BackClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(FlightRecorderAction.SaveClick) }
                        },
                        modifier = Modifier.testTag("SaveButton"),
                    )
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        FlightRecorderContent(
            state = state,
            onDurationSelected = remember(viewModel) {
                { viewModel.trySendAction(FlightRecorderAction.DurationSelect(it)) }
            },
            onHelpCenterClick = remember(viewModel) {
                { viewModel.trySendAction(FlightRecorderAction.HelpCenterClick) }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun FlightRecorderContent(
    state: FlightRecorderState,
    onDurationSelected: (FlightRecorderDuration) -> Unit,
    onHelpCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(state = rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = R.string.experiencing_an_issue),
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = stringResource(
                id = R.string.enable_temporary_logging_to_collect_and_inspect_logs_locally,
            ),
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Text(
            text = stringResource(id = R.string.to_get_started_set_a_logging_duration),
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        DurationSelectButton(
            selectedOption = state.selectedDuration,
            onOptionSelected = onDurationSelected,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = R.string.logs_will_be_automatically_deleted_after_30_days),
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenHyperTextLink(
            annotatedResId = R.string.for_details_on_what_is_and_isnt_logged,
            annotationKey = "helpCenter",
            accessibilityString = stringResource(id = R.string.bitwarden_help_center),
            onClick = onHelpCenterClick,
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DurationSelectButton(
    selectedOption: FlightRecorderDuration,
    onOptionSelected: (FlightRecorderDuration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val options = FlightRecorderDuration.entries.map { it.displayText() }.toImmutableList()
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.logging_duration),
        options = options,
        selectedOption = selectedOption.displayText(),
        onOptionSelected = { selectedOption ->
            onOptionSelected(
                FlightRecorderDuration
                    .entries
                    .first { selectedOption == it.displayText.toString(resources) },
            )
        },
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
