package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar

/**
 * Displays the vault move to organization screen.
 */
@Composable
fun VaultMoveToOrganizationScreen(
    viewModel: VaultMoveToOrganizationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultMoveToOrganizationEvent.NavigateBack -> onNavigateBack()
            is VaultMoveToOrganizationEvent.ShowToast -> {
                Toast.makeText(context, event.text(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }
    VaultMoveToOrganizationScaffold(
        state = state,
        closeClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.BackClick) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultMoveToOrganizationScaffold(
    state: VaultMoveToOrganizationState,
    closeClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.move_to_organization),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = closeClick,
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .padding(innerPadding)

        when (state.viewState) {
            is VaultMoveToOrganizationState.ViewState.Content -> {
                // TODO add real views in BIT-844 UI
                Text(text = "Content")
            }
            is VaultMoveToOrganizationState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = state.viewState.message(),
                    modifier = modifier,
                )
            }
            is VaultMoveToOrganizationState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = modifier)
            }
        }
    }
}
