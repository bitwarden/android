package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialColors
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography

/**
 * Displays the pending login requests screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun PendingRequestsScreen(
    viewModel: PendingRequestsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsState()
    val context = LocalContext.current
    val resources = context.resources
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PendingRequestsEvent.NavigateBack -> onNavigateBack()

            is PendingRequestsEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.pending_log_in_requests),
                scrollBehavior = scrollBehavior,
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(PendingRequestsAction.CloseClick) }
                },
            )
        },
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is PendingRequestsState.ViewState.Content -> {
                PendingRequestsContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    state = viewState,
                    onDeclineAllRequestsClick = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                PendingRequestsAction.DeclineAllRequestsClick,
                            )
                        }
                    },
                )
            }

            is PendingRequestsState.ViewState.Empty -> PendingRequestsEmpty(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )

            is PendingRequestsState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message.toString(resources),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )

            is PendingRequestsState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            )
        }
    }
}

/**
 * Models the list content for the Pending Requests screen.
 */
@Composable
private fun PendingRequestsContent(
    state: PendingRequestsState.ViewState.Content,
    onDeclineAllRequestsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {

        LazyColumn(
            Modifier.padding(bottom = 16.dp),
        ) {
            items(state.requests) { request ->
                PendingRequestItem(
                    fingerprintPhrase = request.fingerprintPhrase,
                    platform = request.platform,
                    timestamp = request.timestamp,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        BitwardenFilledTonalButtonWithIcon(
            label = stringResource(id = R.string.decline_all_requests),
            icon = painterResource(id = R.drawable.ic_trash),
            onClick = onDeclineAllRequestsClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Represents a pending request item to display in the list.
 */
@Composable
private fun PendingRequestItem(
    fingerprintPhrase: String,
    platform: String,
    timestamp: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.fingerprint_phrase),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = fingerprintPhrase,
            color = LocalNonMaterialColors.current.fingerprint,
            style = LocalNonMaterialTypography.current.sensitiveInfoSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = platform,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Models the empty state for the Pending Requests screen.
 */
@Composable
private fun PendingRequestsEmpty(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_pending_requests),
            contentDescription = null,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.no_pending_requests),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
        Spacer(modifier = Modifier.height(64.dp))
    }
}
