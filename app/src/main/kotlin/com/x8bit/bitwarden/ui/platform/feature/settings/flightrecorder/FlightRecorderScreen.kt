package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import android.content.res.Resources
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.data.manager.model.FlightRecorderDuration
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.text.BitwardenHyperTextLink
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.util.displayText
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
                title = stringResource(id = BitwardenString.enable_flight_recorder_title),
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(FlightRecorderAction.BackClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.save),
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
            text = stringResource(id = BitwardenString.experiencing_an_issue),
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
                id = BitwardenString.enable_temporary_logging_to_collect_and_inspect_logs_locally,
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
            text = stringResource(id = BitwardenString.to_get_started_set_a_logging_duration),
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
            text = stringResource(
                id = BitwardenString.logs_will_be_automatically_deleted_after_30_days,
            ),
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenHyperTextLink(
            annotatedResId = BitwardenString.for_details_on_what_is_and_isnt_logged,
            annotationKey = "helpCenter",
            accessibilityString = stringResource(id = BitwardenString.bitwarden_help_center),
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
    resources: Resources = LocalResources.current,
) {
    val options = FlightRecorderDuration.entries.map { it.displayText() }.toImmutableList()
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.logging_duration),
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
