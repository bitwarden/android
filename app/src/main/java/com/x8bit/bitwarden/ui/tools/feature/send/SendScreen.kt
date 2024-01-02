package com.x8bit.bitwarden.ui.tools.feature.send

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.components.BitwardenMediumTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import kotlinx.collections.immutable.persistentListOf

/**
 * UI for the send screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onNavigateNewSend: () -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
    intentHandler: IntentHandler = IntentHandler(context = LocalContext.current),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is SendEvent.NavigateNewSend -> onNavigateNewSend()

            is SendEvent.NavigateToAboutSend -> {
                intentHandler.launchUri("https://bitwarden.com/products/send".toUri())
            }

            is SendEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenMediumTopAppBar(
                title = stringResource(id = R.string.send),
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_sends),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(SendAction.SearchClick) }
                        },
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.sync),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.SyncClick) }
                                },
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.lock),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.LockClick) }
                                },
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.about_send),
                                onClick = remember(viewModel) {
                                    { viewModel.trySendAction(SendAction.AboutSendClick) }
                                },
                            ),
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = true,
                // The enter transition is required for AnimatedVisibility to work correctly on
                // FloatingActionButton. See - https://issuetracker.google.com/issues/224005027?pli=1
                enter = fadeIn() + expandIn { IntSize(width = 1, height = 1) },
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(SendAction.AddSendClick) }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = stringResource(id = R.string.add_item),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                SendState.Empty -> SendEmpty(
                    remember(viewModel) {
                        { viewModel.trySendAction(SendAction.AddSendClick) }
                    },
                )
            }
        }
    }
}
