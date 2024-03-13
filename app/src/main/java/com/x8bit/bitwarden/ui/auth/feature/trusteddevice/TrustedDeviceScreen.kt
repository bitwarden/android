package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.trusteddevice.handlers.TrustedDeviceHandlers
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold

/**
 * The top level composable for the Reset Password screen.
 */
@Composable
fun TrustedDeviceScreen(
    viewModel: TrustedDeviceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = remember(viewModel) { TrustedDeviceHandlers.create(viewModel = viewModel) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            TrustedDeviceEvent.NavigateBack -> onNavigateBack()
        }
    }

    TrustedDeviceScaffold(
        state = state,
        handlers = handlers,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrustedDeviceScaffold(
    state: TrustedDeviceState,
    handlers: TrustedDeviceHandlers,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.log_in_initiated),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = R.drawable.ic_close),
                    navigationIconContentDescription = stringResource(id = R.string.close),
                    onNavigationIconClick = handlers.onBackClick,
                ),
            )
        },
    ) { innerPadding ->
    }
}
