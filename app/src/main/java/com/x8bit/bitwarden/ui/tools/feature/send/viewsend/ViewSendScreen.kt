package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter

/**
 * Displays view send screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSendScreen(
    viewModel: ViewSendViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditSend: (sendId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ViewSendEvent.NavigateBack -> onNavigateBack()
            is ViewSendEvent.NavigateToEdit -> onNavigateToEditSend(event.sendId)
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                    navigationIconContentDescription = stringResource(id = R.string.close),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(ViewSendAction.CloseClick) }
                    },
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isFabVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                BitwardenFloatingActionButton(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(ViewSendAction.EditClick) }
                    },
                    painter = rememberVectorPainter(id = R.drawable.ic_pencil),
                    contentDescription = stringResource(id = R.string.edit_send),
                    modifier = Modifier.testTag(tag = "EditItemButton"),
                )
            }
        },
    ) {
        ViewSendScreenContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ViewSendScreenContent(
    state: ViewSendState,
    modifier: Modifier = Modifier,
) {
    when (val viewState = state.viewState) {
        ViewSendState.ViewState.Content -> {
            // TODO: Build out the UI (PM-21135)
        }

        is ViewSendState.ViewState.Error -> {
            BitwardenErrorContent(
                message = viewState.message(),
                modifier = modifier,
            )
        }

        ViewSendState.ViewState.Loading -> {
            BitwardenLoadingContent(modifier = modifier)
        }
    }
}
