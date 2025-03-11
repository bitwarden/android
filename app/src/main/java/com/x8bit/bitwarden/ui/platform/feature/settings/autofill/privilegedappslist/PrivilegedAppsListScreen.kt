package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedappslist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedappslist.model.PrivilegedAppListItem
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Top level composable for the privileged apps list.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivilegedAppsListScreen(
    onNavigateBack: () -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: PrivilegedAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel) { event ->
        when (event) {
            PrivilegedAppsListEvent.NavigateBack -> onNavigateBack()
            is PrivilegedAppsListEvent.NavigateToUri -> {
                intentManager.launchUri(uri = event.uri)
            }
        }
    }

    when (val dialogState = state.dialogState) {
        is Fido2TrustState.DialogState.Loading -> {
            BitwardenLoadingDialog(stringResource(R.string.loading))
        }

        is Fido2TrustState.DialogState.ConfirmLaunchUri -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title.invoke(),
                message = dialogState.message.invoke(),
                confirmButtonText = stringResource(R.string.continue_text),
                dismissButtonText = stringResource(R.string.cancel),
                onConfirmClick = remember {
                    {
                        viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick)
                        intentManager.launchUri(dialogState.uri)
                    }
                },
                onDismissClick = remember {
                    { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
                },
                onDismissRequest = remember {
                    { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
                },
            )
        }

        is Fido2TrustState.DialogState.ConfirmDeleteTrustedApp -> {
            BitwardenTwoButtonDialog(
                title = stringResource(R.string.delete),
                message = stringResource(
                    R.string.are_you_sure_you_want_to_stop_trusting_x,
                    dialogState.app.packageName,
                ),
                confirmButtonText = stringResource(R.string.ok),
                dismissButtonText = stringResource(R.string.cancel),
                onConfirmClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick(
                                app = dialogState.app,
                            ),
                        )
                    }
                },
                onDismissClick = remember(viewModel) {
                    { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
                },
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
                },
            )
        }

        is Fido2TrustState.DialogState.General -> {
            BitwardenBasicDialog(
                title = stringResource(R.string.an_error_has_occurred),
                message = dialogState.message.invoke(),
                onDismissRequest = remember(viewModel) {
                    { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
                },
            )
        }

        null -> Unit
    }

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.privileged_apps),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(PrivilegedAppsListAction.BackClick) }
                },
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_external_link,
                        contentDescription = stringResource(R.string.bitwarden_help_center),
                        onClick = remember(viewModel) {
                            {
                                viewModel.trySendAction(
                                    action = PrivilegedAppsListAction.LaunchHelpCenterClick,
                                )
                            }
                        },
                    )
                },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        PrivilegedAppsListContent(
            state = state,
            onDeleteClick = remember(viewModel) {
                { viewModel.trySendAction(PrivilegedAppsListAction.UserTrustedAppDeleteClick(it)) }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun PrivilegedAppsListContent(
    state: Fido2TrustState,
    onDeleteClick: (PrivilegedAppListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        if (state.userTrustedApps.isNotEmpty()) {
            item(key = "trusted_by_you") {
                Spacer(modifier = Modifier.height(12.dp))
                PrivilegedAppHeaderItem(
                    headerText = stringResource(R.string.trusted_by_you),
                    learnMoreText = stringResource(R.string.trusted_by_you_learn_more),
                    itemCount = state.userTrustedApps.size,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(
                key = { _, item -> "userTrust_${item.packageName}_${item.signature}" },
                items = state.userTrustedApps,
            ) { index, item ->
                PrivilegedAppListItem(
                    item = item,
                    canDelete = true,
                    onClick = remember(item) {
                        { onDeleteClick(item) }
                    },
                    cardStyle = state.userTrustedApps
                        .toListItemCardStyle(index = index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
        }

        if (state.communityTrustedApps.isNotEmpty()) {
            item(key = "trusted_by_community") {
                Spacer(modifier = Modifier.height(12.dp))
                PrivilegedAppHeaderItem(
                    headerText = stringResource(R.string.trusted_by_the_community),
                    learnMoreText = stringResource(R.string.trusted_by_community_learn_more),
                    itemCount = state.communityTrustedApps.size,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(
                key = { _, item -> "communityTrust_${item.packageName}_${item.signature}" },
                items = state.communityTrustedApps,
            ) { index, item ->
                PrivilegedAppListItem(
                    item = item,
                    canDelete = false,
                    onClick = {},
                    cardStyle = state.communityTrustedApps
                        .toListItemCardStyle(index = index),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        if (state.googleTrustedApps.isNotEmpty()) {
            item(key = "trusted_by_google") {
                Spacer(modifier = Modifier.height(12.dp))
                PrivilegedAppHeaderItem(
                    headerText = stringResource(R.string.trusted_by_google),
                    learnMoreText = stringResource(R.string.trusted_by_google_learn_more),
                    itemCount = state.googleTrustedApps.size,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(
                key = { _, item -> "googleTrust_${item.packageName}_${item.signature}" },
                items = state.googleTrustedApps,
            ) { index, item ->
                PrivilegedAppListItem(
                    item = item,
                    canDelete = false,
                    onClick = { },
                    cardStyle = state.googleTrustedApps
                        .toListItemCardStyle(index),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LazyItemScope.PrivilegedAppHeaderItem(
    headerText: String,
    learnMoreText: String,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .standardHorizontalMargin(),
    ) {
        BitwardenListHeaderText(
            label = headerText,
            supportingLabel = itemCount.toString(),
            modifier = Modifier.animateItem(),
        )
        val size by animateDpAsState(
            targetValue = 16.dp,
            label = "${headerText}_animation",
        )
        Spacer(modifier = Modifier.width(width = 8.dp))
        BitwardenStandardIconButton(
            vectorIconRes = R.drawable.ic_question_circle_small,
            contentDescription = "",
            onClick = { showDialog = !showDialog },
            contentColor = BitwardenTheme.colorScheme.icon.secondary,
            modifier = Modifier.size(size),
        )
    }

    if (showDialog) {
        BitwardenBasicDialog(
            title = headerText,
            message = learnMoreText,
            onDismissRequest = { showDialog = false },
        )
    }
}

@Composable
private fun LazyItemScope.PrivilegedAppListItem(
    item: PrivilegedAppListItem,
    canDelete: Boolean,
    onClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                paddingStart = 16.dp,
                paddingEnd = if (canDelete) 4.dp else 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier,
                text = item.packageName,
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.signature,
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canDelete) {
            BitwardenStandardIconButton(
                vectorIconRes = R.drawable.ic_send_pending_delete,
                contentDescription = "",
                onClick = onClick,
            )
        }
    }
}

@Preview
@Composable
private fun PrivilegedAppsListScreenPreview() {
    PrivilegedAppsListContent(
        state = Fido2TrustState(
            googleTrustedApps = listOf<PrivilegedAppListItem>(
                PrivilegedAppListItem(
                    packageName = "com.x8bit.bitwarden.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.bitwarden.authenticator.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
            )
                .toImmutableList(),
            communityTrustedApps = listOf<PrivilegedAppListItem>(
                PrivilegedAppListItem(
                    packageName = "com.x8bit.bitwarden.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.bitwarden.authenticator.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
            )
                .toImmutableList(),
            userTrustedApps = listOf<PrivilegedAppListItem>(
                PrivilegedAppListItem(
                    packageName = "com.x8bit.bitwarden.you",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.bitwarden.authenticator.you",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel.you",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
            )
                .toImmutableList(),
            dialogState = null,
        ),
        onDeleteClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PrivilegedAppListItemPreview() {
    LazyColumn {
        item {
            PrivilegedAppListItem(
                item = PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                ),
                canDelete = false,
                onClick = {},
                cardStyle = CardStyle.Middle(hasDivider = false),
            )
        }
    }
}
