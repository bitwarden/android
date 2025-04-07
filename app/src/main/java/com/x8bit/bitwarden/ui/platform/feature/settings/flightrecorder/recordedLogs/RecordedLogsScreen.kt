package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter

/**
 * Displays the flight recorder recorded logs screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordedLogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordedLogsViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel) { event ->
        when (event) {
            RecordedLogsEvent.NavigateBack -> onNavigateBack()
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.recorded_logs_title),
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(RecordedLogsAction.BackClick) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        // TODO: PM-19593 Create the flight recorder UI.
    }
}
