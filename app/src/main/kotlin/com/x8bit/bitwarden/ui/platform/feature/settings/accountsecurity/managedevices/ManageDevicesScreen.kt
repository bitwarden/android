package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.rememberBitwardenPullToRefreshState
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager

/**
 * Displays the Manage Devices screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ManageDevicesScreen(
    viewModel: ManageDevicesViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateBack: () -> Unit,
    onNavigateToLoginApproval: (fingerprint: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.trySendAction(ManageDevicesAction.RefreshPull) },
    )
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ManageDevicesEvent.NavigateBack -> onNavigateBack()
            is ManageDevicesEvent.NavigateToLoginApproval -> {
                onNavigateToLoginApproval(event.fingerprint)
            }

            is ManageDevicesEvent.ShowSnackbar ->
                snackbarHostState.showSnackbar(event.data)
        }
    }

    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.trySendAction(ManageDevicesAction.LifecycleResume)
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
        onDismiss = { viewModel.trySendAction(ManageDevicesAction.HideBottomSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) { animatedOnDismiss ->
        ManageDevicesBottomSheetContent(
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
                title = stringResource(id = BitwardenString.manage_devices),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = {
                    viewModel.trySendAction(ManageDevicesAction.CloseClick)
                },
            )
        },
        pullToRefreshState = pullToRefreshState,
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
    ) {
        when (val viewState = state.viewState) {
            is ManageDevicesState.ViewState.Content -> {
                ManageDevicesContent(
                    modifier = Modifier.fillMaxSize(),
                    state = viewState,
                    onNavigateToLoginApproval = {
                        viewModel.trySendAction(ManageDevicesAction.PendingRequestRowClick(it))
                    },
                )
            }

            ManageDevicesState.ViewState.Error -> BitwardenErrorContent(
                message = stringResource(
                    id = BitwardenString.generic_error_message,
                ),
                modifier = Modifier.fillMaxSize(),
            )

            ManageDevicesState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Models the list content for the Manage Devices screen.
 */
@Composable
private fun ManageDevicesContent(
    state: ManageDevicesState.ViewState.Content,
    onNavigateToLoginApproval: (fingerprint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
        itemsIndexed(state.items) { index, item ->
            when (item.status) {
                DeviceSessionStatus.Pending -> item.fingerprintPhrase?.let {
                    PendingRequestItem(
                        fingerprintPhrase = item.fingerprintPhrase,
                        platform = item.typeName(),
                        firstLoginDate = item.firstLoginDate,
                        isTrusted = item.isTrusted,
                        onNavigateToLoginApproval = onNavigateToLoginApproval,
                        cardStyle = state.items.toListItemCardStyle(
                            index = index,
                            dividerPadding = 0.dp,
                        ),
                        modifier = Modifier
                            .testTag("LoginRequestCell")
                            .fillMaxWidth()
                            .standardHorizontalMargin(),
                    )
                }

                DeviceSessionStatus.None,
                DeviceSessionStatus.Current,
                    -> {
                    SessionItem(
                        platform = item.typeName(),
                        firstLoginDate = item.firstLoginDate,
                        lastActivityLabel = item.lastActivityLabel,
                        status = item.status,
                        isTrusted = item.isTrusted,
                        cardStyle = state.items.toListItemCardStyle(
                            index = index,
                            dividerPadding = 0.dp,
                        ),
                        modifier = Modifier
                            .testTag("CurrentItemCell")
                            .fillMaxWidth()
                            .standardHorizontalMargin(),
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Represents a pending request item to display in the list.
 */
@Composable
private fun PendingRequestItem(
    fingerprintPhrase: String,
    platform: String,
    firstLoginDate: String,
    isTrusted: Boolean,
    onNavigateToLoginApproval: (fingerprint: String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = { onNavigateToLoginApproval(fingerprintPhrase) },
                paddingHorizontal = 16.dp,
            ),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = platform,
                style = BitwardenTheme.typography.titleMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                textAlign = TextAlign.Start,
            )
            if (isTrusted) {
                Text(
                    text = stringResource(id = BitwardenString.trusted),
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    textAlign = TextAlign.Start,
                )
            }
            Spacer(Modifier.height(height = 8.dp))
            DeviceStatusIndicatorRow(
                label = stringResource(id = BitwardenString.pending_request),
                color = BitwardenTheme.colorScheme.status.weak2,
            )
            DeviceInfoAnnotatedLabel(
                id = BitwardenString.first_login_date,
                arg = firstLoginDate,
            )
        }
        Icon(
            painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_right),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .mirrorIfRtl()
                .size(size = 16.dp),
        )
    }
}

/**
 * Represents a registered session item to display in the list.
 */
@Composable
private fun SessionItem(
    platform: String,
    firstLoginDate: String,
    lastActivityLabel: Text?,
    status: DeviceSessionStatus,
    isTrusted: Boolean,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                paddingHorizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = platform,
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Start,
        )
        if (isTrusted) {
            Text(
                text = stringResource(id = BitwardenString.trusted),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                textAlign = TextAlign.Start,
            )
        }
        Spacer(Modifier.height(height = 8.dp))
        if (status == DeviceSessionStatus.Current) {
            DeviceStatusIndicatorRow(
                label = stringResource(id = BitwardenString.current_session),
                color = BitwardenTheme.colorScheme.status.strong,
            )
        } else {
            lastActivityLabel?.let {
                DeviceInfoAnnotatedLabel(
                    id = BitwardenString.recently_active,
                    arg = it(),
                )
            }
        }
        DeviceInfoAnnotatedLabel(
            id = BitwardenString.first_login_date,
            arg = firstLoginDate,
        )
    }
}

/**
 * Displays a colored dot followed by [label] in a horizontal row, used to indicate device status.
 */
@Composable
private fun DeviceStatusIndicatorRow(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = BitwardenTheme.typography.bodySmall,
            color = color,
        )
    }
}

/**
 * Displays an annotated string resource with [arg] substituted in, using secondary text color and
 * a bold emphasis style for the dynamic portion.
 */
@Composable
private fun DeviceInfoAnnotatedLabel(
    @StringRes id: Int,
    arg: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = annotatedStringResource(
            id = id,
            args = arrayOf(arg),
            emphasisHighlightStyle = SpanStyle(
                color = BitwardenTheme.colorScheme.text.secondary,
                fontSize = BitwardenTheme.typography.bodySmall.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            style = SpanStyle(
                color = BitwardenTheme.colorScheme.text.secondary,
                fontSize = BitwardenTheme.typography.bodySmall.fontSize,
            ),
        ),
        textAlign = TextAlign.Start,
        modifier = modifier,
    )
}

@Composable
private fun ManageDevicesBottomSheetContent(
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
