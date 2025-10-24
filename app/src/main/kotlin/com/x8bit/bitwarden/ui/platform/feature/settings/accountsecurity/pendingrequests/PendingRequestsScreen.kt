package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager

/**
 * Displays the pending login requests screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun PendingRequestsScreen(
    viewModel: PendingRequestsViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprint: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = remember(viewModel) {
            { viewModel.trySendAction(PendingRequestsAction.RefreshPull) }
        },
    )
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PendingRequestsEvent.NavigateBack -> onNavigateBack()
            is PendingRequestsEvent.NavigateToLoginApproval -> {
                onNavigateToLoginApproval(event.fingerprint)
            }

            is PendingRequestsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.trySendAction(PendingRequestsAction.LifecycleResume)
            }

            else -> Unit
        }
    }

    val hideBottomSheet = state.hideBottomSheet ||
        permissionsManager.checkPermission(Manifest.permission.POST_NOTIFICATIONS) ||
        permissionsManager.shouldShowRequestPermissionRationale(
            permission = Manifest.permission.POST_NOTIFICATIONS,
        )
    BitwardenModalBottomSheet(
        showBottomSheet = !hideBottomSheet,
        sheetTitle = stringResource(BitwardenString.enable_notifications),
        onDismiss = remember(viewModel) {
            { viewModel.trySendAction(PendingRequestsAction.HideBottomSheet) }
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) { animatedOnDismiss ->
        PendingRequestsBottomSheetContent(
            permissionsManager = permissionsManager,
            onDismiss = animatedOnDismiss,
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.pending_log_in_requests),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(PendingRequestsAction.CloseClick) }
                },
            )
        },
        pullToRefreshState = pullToRefreshState,
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        when (val viewState = state.viewState) {
            is PendingRequestsState.ViewState.Content -> {
                PendingRequestsContent(
                    modifier = Modifier.fillMaxSize(),
                    state = viewState,
                    onDeclineAllRequestsConfirm = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                PendingRequestsAction.DeclineAllRequestsConfirm,
                            )
                        }
                    },
                    onNavigateToLoginApproval = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                PendingRequestsAction.PendingRequestRowClick(it),
                            )
                        }
                    },
                )
            }

            is PendingRequestsState.ViewState.Empty -> PendingRequestsEmpty(
                modifier = Modifier.fillMaxSize(),
            )

            PendingRequestsState.ViewState.Error -> BitwardenErrorContent(
                message = stringResource(BitwardenString.generic_error_message),
                modifier = Modifier.fillMaxSize(),
            )

            PendingRequestsState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier.fillMaxSize(),
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
    onDeclineAllRequestsConfirm: () -> Unit,
    onNavigateToLoginApproval: (fingerprint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        var shouldShowDeclineAllRequestsConfirm by remember { mutableStateOf(false) }

        if (shouldShowDeclineAllRequestsConfirm) {
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.decline_all_requests),
                message = stringResource(
                    id = BitwardenString
                        .are_you_sure_you_want_to_decline_all_pending_log_in_requests,
                ),
                confirmButtonText = stringResource(BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = {
                    onDeclineAllRequestsConfirm()
                    shouldShowDeclineAllRequestsConfirm = false
                },
                onDismissClick = { shouldShowDeclineAllRequestsConfirm = false },
                onDismissRequest = { shouldShowDeclineAllRequestsConfirm = false },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(weight = 1f, fill = false),
        ) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
            }
            itemsIndexed(state.requests) { index, request ->
                PendingRequestItem(
                    fingerprintPhrase = request.fingerprintPhrase,
                    platform = request.platform,
                    timestamp = request.timestamp,
                    onNavigateToLoginApproval = onNavigateToLoginApproval,
                    cardStyle = state.requests.toListItemCardStyle(
                        index = index,
                        dividerPadding = 0.dp,
                    ),
                    modifier = Modifier
                        .testTag("LoginRequestCell")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }
        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.decline_all_requests),
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_trash),
            onClick = { shouldShowDeclineAllRequestsConfirm = true },
            modifier = Modifier
                .testTag("DeclineAllRequestsButton")
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))
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
    onNavigateToLoginApproval: (fingerprintPhrase: String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = { onNavigateToLoginApproval(fingerprintPhrase) },
                paddingHorizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = BitwardenString.fingerprint_phrase),
            style = BitwardenTheme.typography.labelMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = fingerprintPhrase,
            color = BitwardenTheme.colorScheme.text.codePink,
            style = BitwardenTheme.typography.sensitiveInfoSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .testTag("FingerprintValueLabel")
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = platform,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.width(width = 16.dp))
            Text(
                text = timestamp,
                style = BitwardenTheme.typography.labelSmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                textAlign = TextAlign.End,
            )
        }
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_pending_requests),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(size = 124.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = BitwardenString.no_pending_requests),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun PendingRequestsBottomSheetContent(
    permissionsManager: PermissionsManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notificationPermissionLauncher = permissionsManager.getLauncher {
        onDismiss()
    }
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(height = 24.dp))
        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_2fa),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(size = 132.dp)
                .align(alignment = Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = BitwardenString.log_in_quickly_and_easily_across_devices),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        @Suppress("MaxLineLength")
        Text(
            text = stringResource(
                id = BitwardenString.bitwarden_can_notify_you_each_time_you_receive_a_new_login_request_from_another_device,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.enable_notifications),
            onClick = {
                @SuppressLint("InlinedApi")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.skip_for_now),
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
