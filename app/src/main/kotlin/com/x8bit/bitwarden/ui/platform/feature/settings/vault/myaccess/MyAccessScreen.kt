@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.content.BitwardenEmptyContent
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.segment.BitwardenSegmentedButton
import com.bitwarden.ui.platform.components.segment.SegmentedButtonState
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamBadgeTone
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamItemIcon
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamStatusBadge
import com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt.PamNotificationActionCard
import com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt.rememberPamNotificationPromptState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.format.FormatStyle

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val COUNTDOWN_TICK_MILLIS = 1_000L

/**
 * Displays the user's privileged access requests, with the actions to start, cancel, end and
 * extend access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun MyAccessScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItem: (VaultItemArgs) -> Unit,
    viewModel: MyAccessViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            MyAccessEvent.NavigateBack -> onNavigateBack()
            is MyAccessEvent.NavigateToVaultItem -> onNavigateToVaultItem(event.args)
            is MyAccessEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }
    LifecycleEventEffect { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.trySendAction(MyAccessAction.LifecycleResume)
        }
    }

    val notificationPrompt = rememberPamNotificationPromptState(
        onPermissionGranted = {
            viewModel.trySendAction(MyAccessAction.NotificationPromptDismiss)
        },
        permissionsManager = permissionsManager,
        intentManager = intentManager,
    )
    val showNotificationCard = state.showNotificationPrompt &&
        !notificationPrompt.hasPermission &&
        state.selectedTab == MyAccessState.Tab.CURRENT

    MyAccessDialogs(
        dialogState = state.dialogState,
        onAction = viewModel::trySendAction,
    )

    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.viewState is MyAccessState.ViewState.Content,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.trySendAction(MyAccessAction.RefreshPull) },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.pam_my_access_requests),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = { viewModel.trySendAction(MyAccessAction.BackClick) },
            )
        },
        utilityBar = {
            BitwardenSegmentedButton(
                options = MyAccessState
                    .Tab
                    .entries
                    .map { tab ->
                        SegmentedButtonState(
                            text = stringResource(
                                id = when (tab) {
                                    MyAccessState.Tab.CURRENT -> BitwardenString.pam_tab_current
                                    MyAccessState.Tab.HISTORY -> BitwardenString.pam_tab_history
                                },
                            ),
                            onClick = { viewModel.trySendAction(MyAccessAction.TabClick(tab)) },
                            isChecked = tab == state.selectedTab,
                            testTag = "PamTab${tab.name}",
                        )
                    }
                    .let { persistentListOf(*it.toTypedArray()) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        pullToRefreshState = pullToRefreshState,
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        when (state.viewState) {
            MyAccessState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }

            MyAccessState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.generic_error_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is MyAccessState.ViewState.Content -> {
                MyAccessLoaded(
                    requests = state.visibleRequests,
                    selectedTab = state.selectedTab,
                    showNotificationCard = showNotificationCard,
                    onEnableNotificationsClick = notificationPrompt.onEnableClick,
                    onAction = viewModel::trySendAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MyAccessLoaded(
    requests: List<MyAccessState.RequestItem>,
    selectedTab: MyAccessState.Tab,
    showNotificationCard: Boolean,
    onEnableNotificationsClick: () -> Unit,
    onAction: (MyAccessAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showNotificationCard) {
            PamNotificationActionCard(
                title = stringResource(
                    id = BitwardenString.pam_get_notified_about_your_access_requests,
                ),
                description = stringResource(
                    id = BitwardenString.pam_know_when_a_request_is_approved_denied_or_revoked,
                ),
                onEnableClick = onEnableNotificationsClick,
                onDismissClick = { onAction(MyAccessAction.NotificationPromptDismiss) },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(vertical = 8.dp),
            )
        }
        if (requests.isEmpty()) {
            MyAccessEmpty(selectedTab = selectedTab, modifier = Modifier.weight(1f))
        } else {
            MyAccessContent(
                requests = requests,
                onAction = onAction,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MyAccessContent(
    requests: List<MyAccessState.RequestItem>,
    onAction: (MyAccessAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.testTag("PamRequestList"),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(items = requests, key = { it.id }) { request ->
            MyAccessRequestCard(
                request = request,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun MyAccessRequestCard(
    request: MyAccessState.RequestItem,
    onAction: (MyAccessAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = LocalClock.current
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .testTag("PamRequestCard")
            .clickable { onAction(MyAccessAction.RequestClick(cipherId = request.cipherId)) }
            .cardStyle(cardStyle = CardStyle.Full, paddingHorizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PamItemIcon(
                iconData = request.itemIcon,
                iconTestTag = "PamRequestItemIcon",
            )
            Text(
                text = request.itemName
                    ?: stringResource(id = BitwardenString.pam_item_fallback_name),
                style = BitwardenTheme.typography.titleMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.weight(1f),
            )
            PamStatusBadge(
                label = stringResource(id = request.stage.labelRes()),
                tone = request.stage.badgeTone(),
                modifier = Modifier.testTag("PamRequestStatus"),
            )
        }
        if (request.isExtension) {
            Text(
                text = stringResource(id = BitwardenString.pam_extension_request_label),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
        request.reason?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(id = BitwardenString.pam_reason_value, it),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
            )
        }
        Text(
            text = stringResource(
                id = BitwardenString.pam_submitted_on,
                request.submittedAt.toFormattedDateTimeStyle(
                    dateStyle = FormatStyle.SHORT,
                    timeStyle = FormatStyle.SHORT,
                    clock = clock,
                ),
            ),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.secondary,
        )
        val leaseEndsAt = request.leaseEndsAt
        if (request.stage == MyAccessState.Stage.ACTIVE && leaseEndsAt != null) {
            LeaseCountdown(
                endsAt = leaseEndsAt,
                onFinished = { onAction(MyAccessAction.LeaseCountdownFinished) },
            )
        }
        RequestActions(request = request, onAction = onAction)
    }
}

@Composable
private fun RequestActions(
    request: MyAccessState.RequestItem,
    onAction: (MyAccessAction) -> Unit,
) {
    val leaseId = request.leaseId
    when (request.stage) {
        MyAccessState.Stage.READY -> {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.pam_start_access),
                onClick = { onAction(MyAccessAction.StartAccessClick(request.id)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("PamStartAccessButton"),
            )
            CancelRequestButton(request = request, onAction = onAction)
        }

        MyAccessState.Stage.PENDING -> {
            Spacer(modifier = Modifier.height(8.dp))
            CancelRequestButton(request = request, onAction = onAction)
        }

        MyAccessState.Stage.ACTIVE -> if (leaseId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            if (request.canExtend) {
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.pam_extend_access),
                    onClick = { onAction(MyAccessAction.ExtendAccessClick(leaseId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("PamExtendAccessButton"),
                )
            }
            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.pam_end_access),
                onClick = { onAction(MyAccessAction.EndAccessClick(leaseId)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("PamEndAccessButton"),
            )
        }

        else -> Unit
    }
}

@Composable
private fun CancelRequestButton(
    request: MyAccessState.RequestItem,
    onAction: (MyAccessAction) -> Unit,
) {
    BitwardenOutlinedButton(
        label = stringResource(id = BitwardenString.pam_cancel_request),
        onClick = { onAction(MyAccessAction.CancelRequestClick(request.id)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("PamCancelRequestButton"),
    )
}

/**
 * Shows the time left on a lease, ticking every second, and calls [onFinished] once at zero.
 */
@Composable
private fun LeaseCountdown(
    endsAt: Instant,
    onFinished: () -> Unit,
) {
    val currentOnFinished by rememberUpdatedState(newValue = onFinished)
    var remaining by remember(endsAt) { mutableStateOf(Duration.between(Instant.now(), endsAt)) }
    LaunchedEffect(endsAt) {
        while (true) {
            val left = Duration.between(Instant.now(), endsAt)
            remaining = left
            if (left.isNegative || left.isZero) {
                currentOnFinished()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    Text(
        text = stringResource(
            id = BitwardenString.pam_time_remaining,
            remaining.toCountdownText(),
        ),
        style = BitwardenTheme.typography.labelLarge,
        color = BitwardenTheme.colorScheme.text.primary,
        modifier = Modifier.testTag("PamCountdown"),
    )
}

/**
 * Formats the duration as `H:MM:SS`, never below zero.
 */
internal fun Duration.toCountdownText(): String {
    val totalSeconds = seconds.coerceAtLeast(0L)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val secs = totalSeconds % SECONDS_PER_MINUTE
    return "%d:%02d:%02d".format(hours, minutes, secs)
}

@Composable
private fun MyAccessEmpty(
    selectedTab: MyAccessState.Tab,
    modifier: Modifier = Modifier,
) {
    when (selectedTab) {
        MyAccessState.Tab.CURRENT -> BitwardenEmptyContent(
            title = stringResource(id = BitwardenString.pam_no_current_requests),
            text = stringResource(id = BitwardenString.pam_my_access_empty_message),
            labelTestTag = "PamEmptyText",
            illustrationData = IconData.Local(iconRes = BitwardenDrawable.ill_pending_requests),
            modifier = modifier,
        )

        MyAccessState.Tab.HISTORY -> BitwardenEmptyContent(
            title = stringResource(id = BitwardenString.pam_my_access_history_empty_title),
            text = stringResource(id = BitwardenString.pam_my_access_history_empty_message),
            labelTestTag = "PamEmptyText",
            illustrationData = IconData.Local(iconRes = BitwardenDrawable.ill_vault_items),
            modifier = modifier,
        )
    }
}

private fun MyAccessState.Stage.labelRes(): Int = when (this) {
    MyAccessState.Stage.PENDING -> BitwardenString.pam_status_pending
    MyAccessState.Stage.READY, MyAccessState.Stage.APPROVED -> BitwardenString.pam_status_approved
    MyAccessState.Stage.ACTIVE -> BitwardenString.pam_status_active
    MyAccessState.Stage.DENIED -> BitwardenString.pam_status_denied
    MyAccessState.Stage.CANCELLED -> BitwardenString.pam_status_cancelled
    MyAccessState.Stage.EXPIRED -> BitwardenString.pam_status_expired
    MyAccessState.Stage.REVOKED -> BitwardenString.pam_status_revoked
    MyAccessState.Stage.UNKNOWN -> BitwardenString.pam_status_unknown
}

private fun MyAccessState.Stage.badgeTone(): PamBadgeTone = when (this) {
    MyAccessState.Stage.PENDING -> PamBadgeTone.WARNING
    MyAccessState.Stage.READY, MyAccessState.Stage.APPROVED -> PamBadgeTone.SUCCESS
    MyAccessState.Stage.ACTIVE, MyAccessState.Stage.UNKNOWN -> PamBadgeTone.INFO
    MyAccessState.Stage.DENIED,
    MyAccessState.Stage.CANCELLED,
    MyAccessState.Stage.EXPIRED,
    MyAccessState.Stage.REVOKED,
        -> PamBadgeTone.ERROR
}
