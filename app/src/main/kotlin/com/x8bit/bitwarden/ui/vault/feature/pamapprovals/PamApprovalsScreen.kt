package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.content.BitwardenEmptyContent
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.segment.BitwardenSegmentedButton
import com.bitwarden.ui.platform.components.segment.SegmentedButtonState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt.PamNotificationActionCard
import com.x8bit.bitwarden.ui.vault.feature.pamnotificationprompt.rememberPamNotificationPromptState
import kotlinx.collections.immutable.persistentListOf

/**
 * Displays the access requests waiting for the user's decision and the ones already decided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun PamApprovalsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (requestId: String) -> Unit,
    viewModel: PamApprovalsViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PamApprovalsEvent.NavigateBack -> onNavigateBack()
            is PamApprovalsEvent.NavigateToDetail -> onNavigateToDetail(event.requestId)
        }
    }
    val notificationPrompt = rememberPamNotificationPromptState(
        onPermissionGranted = {
            viewModel.trySendAction(PamApprovalsAction.NotificationPromptDismiss)
        },
        permissionsManager = permissionsManager,
        intentManager = intentManager,
    )
    LifecycleEventEffect { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.trySendAction(PamApprovalsAction.LifecycleResume)
        }
    }

    val showNotificationCard = state.showNotificationPrompt &&
        !notificationPrompt.hasPermission &&
        state.selectedTab == PamApprovalsState.Tab.INBOX

    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.viewState is PamApprovalsState.ViewState.Content,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.trySendAction(PamApprovalsAction.RefreshPull) },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.pam_approvals),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = { viewModel.trySendAction(PamApprovalsAction.BackClick) },
            )
        },
        utilityBar = {
            BitwardenSegmentedButton(
                options = PamApprovalsState
                    .Tab
                    .entries
                    .map { tab ->
                        SegmentedButtonState(
                            text = stringResource(id = tab.labelRes()),
                            onClick = {
                                viewModel.trySendAction(PamApprovalsAction.TabClick(tab))
                            },
                            isChecked = tab == state.selectedTab,
                            testTag = "PamApprovalsTab${tab.name}",
                        )
                    }
                    .let { persistentListOf(*it.toTypedArray()) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        pullToRefreshState = pullToRefreshState,
    ) {
        when (state.viewState) {
            PamApprovalsState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }

            PamApprovalsState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.pam_approvals_load_failed),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is PamApprovalsState.ViewState.Content -> PamApprovalsLoaded(
                requests = state.visibleRequests,
                selectedTab = state.selectedTab,
                showNotificationCard = showNotificationCard,
                onEnableNotificationsClick = notificationPrompt.onEnableClick,
                onDismissNotificationsClick = {
                    viewModel.trySendAction(PamApprovalsAction.NotificationPromptDismiss)
                },
                onRequestClick = { viewModel.trySendAction(PamApprovalsAction.RequestClick(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PamApprovalsLoaded(
    requests: List<PamApprovalsState.RequestItem>,
    selectedTab: PamApprovalsState.Tab,
    showNotificationCard: Boolean,
    onEnableNotificationsClick: () -> Unit,
    onDismissNotificationsClick: () -> Unit,
    onRequestClick: (requestId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showNotificationCard) {
            PamNotificationActionCard(
                title = stringResource(id = BitwardenString.pam_enable_notifications_title),
                description = stringResource(
                    id = BitwardenString.pam_enable_notifications_description,
                ),
                onEnableClick = onEnableNotificationsClick,
                onDismissClick = onDismissNotificationsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(top = 12.dp),
            )
        }
        if (requests.isEmpty()) {
            PamApprovalsEmpty(
                selectedTab = selectedTab,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            PamApprovalsContent(
                requests = requests,
                onRequestClick = onRequestClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PamApprovalsContent(
    requests: List<PamApprovalsState.RequestItem>,
    onRequestClick: (requestId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.testTag("PamApprovalList"),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(items = requests, key = { it.id }) { request ->
            PamApprovalCard(
                request = request,
                onClick = { onRequestClick(request.id) },
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

@Composable
private fun PamApprovalCard(
    request: PamApprovalsState.RequestItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .testTag("PamApprovalCard")
            .cardStyle(
                cardStyle = CardStyle.Full,
                onClick = onClick,
                paddingHorizontal = 16.dp,
            ),
    ) {
        PamApprovalCardHeader(request = request)
        request.collectionName?.let {
            Text(
                text = it,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
        Text(
            text = request.requesterName
                ?: stringResource(id = BitwardenString.pam_unknown_requester),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.testTag("PamApprovalRequester"),
        )
        Text(
            text = pamWindowText(start = request.windowStart, end = request.windowEnd),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.secondary,
        )
        if (request.isOwnRequest) {
            Text(
                text = stringResource(id = BitwardenString.pam_cannot_decide_own),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
    }
}

@Composable
private fun PamApprovalCardHeader(
    request: PamApprovalsState.RequestItem,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        PamItemIcon(
            iconData = request.itemIcon,
            iconTestTag = "PamApprovalItemIcon",
        )
        Text(
            text = request.itemName
                ?: stringResource(id = BitwardenString.pam_item_fallback_name),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.weight(1f),
        )
        PamStatusBadge(
            label = stringResource(id = request.status.labelRes()),
            tone = request.status.badgeTone(),
            modifier = Modifier.testTag("PamApprovalStatus"),
        )
    }
}

@Composable
private fun PamApprovalsEmpty(
    selectedTab: PamApprovalsState.Tab,
    modifier: Modifier = Modifier,
) {
    when (selectedTab) {
        PamApprovalsState.Tab.INBOX -> BitwardenEmptyContent(
            title = stringResource(id = BitwardenString.pam_inbox_zero),
            titleTestTag = "PamInboxZeroTitle",
            text = stringResource(id = BitwardenString.pam_inbox_empty_description),
            illustrationData = IconData.Local(iconRes = BitwardenDrawable.ill_celebration),
            modifier = modifier,
        )

        PamApprovalsState.Tab.HISTORY -> BitwardenEmptyContent(
            title = stringResource(id = BitwardenString.pam_approvals_history_empty_title),
            text = stringResource(id = BitwardenString.pam_approvals_history_empty_message),
            labelTestTag = "PamApprovalsHistoryEmpty",
            illustrationData = IconData.Local(iconRes = BitwardenDrawable.ill_vault_items),
            modifier = modifier,
        )
    }
}

private fun PamApprovalsState.Tab.labelRes(): Int = when (this) {
    PamApprovalsState.Tab.INBOX -> BitwardenString.pam_approvals_inbox_tab
    PamApprovalsState.Tab.HISTORY -> BitwardenString.pam_tab_history
}

internal fun PamRequestStatus.labelRes(): Int = when (this) {
    PamRequestStatus.PENDING -> BitwardenString.pam_status_pending
    PamRequestStatus.APPROVED -> BitwardenString.pam_status_approved
    PamRequestStatus.DENIED -> BitwardenString.pam_status_denied
    PamRequestStatus.CANCELLED -> BitwardenString.pam_status_cancelled
    PamRequestStatus.EXPIRED -> BitwardenString.pam_status_expired
    PamRequestStatus.UNKNOWN -> BitwardenString.pam_status_unknown
}
