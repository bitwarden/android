package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

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
        moveClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick) }
        },
        dismissClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.DismissClick) }
        },
        organizationSelect = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.OrganizationSelect(it)) }
        },
        collectionSelect = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.CollectionSelect(it)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun VaultMoveToOrganizationScaffold(
    state: VaultMoveToOrganizationState,
    closeClick: () -> Unit,
    moveClick: () -> Unit,
    dismissClick: () -> Unit,
    organizationSelect: (VaultMoveToOrganizationState.ViewState.Content.Organization) -> Unit,
    collectionSelect: (VaultCollection) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    when (val dialog = state.dialogState) {
        is VaultMoveToOrganizationState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = dialog.message,
                ),
                onDismissRequest = dismissClick,
            )
        }

        is VaultMoveToOrganizationState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = dialog.message,
                ),
            )
        }

        null -> Unit
    }
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
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.move),
                        onClick = moveClick,
                        isEnabled = state.viewState is
                            VaultMoveToOrganizationState.ViewState.Content,
                    )
                },
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .padding(innerPadding)

        when (state.viewState) {
            is VaultMoveToOrganizationState.ViewState.Content -> {
                VaultMoveToOrganizationContent(
                    state = state.viewState,
                    organizationSelect = organizationSelect,
                    collectionSelect = collectionSelect,
                    modifier = modifier,
                )
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

            is VaultMoveToOrganizationState.ViewState.Empty -> {
                VaultMoveToOrganizationEmpty(modifier = modifier)
            }
        }
    }
}
