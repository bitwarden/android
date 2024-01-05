package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch

/**
 * Displays the other screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(
    onNavigateBack: () -> Unit,
    viewModel: OtherViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            OtherEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.other),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(OtherAction.BackClick) }
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
            BitwardenWideSwitch(
                label = stringResource(id = R.string.enable_sync_on_refresh),
                description = stringResource(id = R.string.enable_sync_on_refresh_description),
                isChecked = state.allowSyncOnRefresh,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(OtherAction.AllowSyncToggle(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenFilledTonalButton(
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(OtherAction.SyncNowButtonClick) }
                },
                label = stringResource(id = R.string.sync_now),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = stringResource(id = R.string.last_sync),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 2.dp),
                )
                Text(
                    text = state.lastSyncTime,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ClearClipboardFrequencyRow(
                currentSelection = state.clearClipboardFrequency,
                onFrequencySelection = remember(viewModel) {
                    { viewModel.trySendAction(OtherAction.ClearClipboardFrequencyChange(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            ScreenCaptureRow(
                currentValue = state.allowScreenCapture,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(OtherAction.AllowScreenCaptureToggle(it)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun ScreenCaptureRow(
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowScreenCaptureConfirmDialog by remember { mutableStateOf(false) }

    BitwardenWideSwitch(
        label = stringResource(id = R.string.allow_screen_capture),
        isChecked = currentValue,
        onCheckedChange = {
            if (currentValue) {
                onValueChange(false)
            } else {
                shouldShowScreenCaptureConfirmDialog = true
            }
        },
        modifier = modifier,
    )

    if (shouldShowScreenCaptureConfirmDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(R.string.allow_screen_capture),
            message = stringResource(R.string.are_you_sure_you_want_to_enable_screen_capture),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                onValueChange(true)
                shouldShowScreenCaptureConfirmDialog = false
            },
            onDismissClick = { shouldShowScreenCaptureConfirmDialog = false },
            onDismissRequest = { shouldShowScreenCaptureConfirmDialog = false },
        )
    }
}

@Composable
private fun ClearClipboardFrequencyRow(
    currentSelection: OtherState.ClearClipboardFrequency,
    onFrequencySelection: (OtherState.ClearClipboardFrequency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowClearClipboardDialog by remember { mutableStateOf(false) }

    BitwardenTextRow(
        text = stringResource(id = R.string.clear_clipboard),
        description = stringResource(id = R.string.clear_clipboard_description),
        onClick = { shouldShowClearClipboardDialog = true },
        modifier = modifier,
    ) {
        Text(
            text = currentSelection.text(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (shouldShowClearClipboardDialog) {
        BitwardenSelectionDialog(
            title = stringResource(id = R.string.clear_clipboard),
            onDismissRequest = { shouldShowClearClipboardDialog = false },
        ) {
            OtherState.ClearClipboardFrequency.entries.forEach { option ->
                BitwardenSelectionRow(
                    text = option.text,
                    isSelected = option == currentSelection,
                    onClick = {
                        shouldShowClearClipboardDialog = false
                        onFrequencySelection(
                            OtherState.ClearClipboardFrequency.entries.first { it == option },
                        )
                    },
                )
            }
        }
    }
}
