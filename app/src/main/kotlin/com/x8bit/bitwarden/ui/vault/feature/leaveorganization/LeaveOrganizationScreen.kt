package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledErrorButton
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.handlers.rememberLeaveOrganizationHandler

/**
 * Top-level composable for the Leave Organization screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveOrganizationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
    viewModel: LeaveOrganizationViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = rememberLeaveOrganizationHandler(viewModel)

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LeaveOrganizationEvent.NavigateBack -> onNavigateBack()
            LeaveOrganizationEvent.NavigateToVault -> onNavigateToVault()
            is LeaveOrganizationEvent.LaunchUri -> {
                intentManager.launchUri(event.uri.toUri())
            }
        }
    }

    LeaveOrganizationDialogs(
        dialogState = state.dialogState,
        onDismissRequest = handlers.onDismissDialog,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.leave_organization),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = handlers.onBackClick,
            )
        },
    ) {
        LeaveOrganizationContent(
            state = state,
            onLeaveClick = handlers.onLeaveClick,
            onHelpLinkClick = handlers.onHelpClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LeaveOrganizationDialogs(
    dialogState: LeaveOrganizationState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        LeaveOrganizationState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                text = stringResource(id = BitwardenString.loading),
            )
        }

        is LeaveOrganizationState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialogState.message(),
                throwable = dialogState.error,
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun LeaveOrganizationContent(
    state: LeaveOrganizationState,
    onLeaveClick: () -> Unit,
    onHelpLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_leave_organization),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(
                id = BitwardenString.are_you_sure_you_want_to_leave_organization,
                state.organizationName,
            ),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = BitwardenString.leave_organization_warning),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledErrorButton(
            label = stringResource(
                id = BitwardenString.leave_organization_button,
                state.organizationName,
            ),
            onClick = onLeaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        BitwardenTextButton(
            label = stringResource(id = BitwardenString.how_to_manage_my_vault),
            onClick = onHelpLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun LeaveOrganizationScreen_preview() {
    BitwardenTheme {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        BitwardenScaffold(
            topBar = {
                BitwardenTopAppBar(
                    title = "Leave organization",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = "Back",
                    onNavigationIconClick = {},
                )
            },
        ) {
            LeaveOrganizationContent(
                state = LeaveOrganizationState(
                    organizationId = "",
                    organizationName = "Test Organization",
                    dialogState = null,
                ),
                onLeaveClick = {},
                onHelpLinkClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
