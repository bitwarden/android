package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model.PrivilegedAppListItem
import kotlinx.collections.immutable.persistentListOf

/**
 * Top level composable for the privileged apps list.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivilegedAppsListScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrivilegedAppsListViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel) { event ->
        when (event) {
            PrivilegedAppsListEvent.NavigateBack -> onNavigateBack()
        }
    }

    PrivilegedAppListDialogs(
        state = state,
        onDismissDialogClick = remember(viewModel) {
            { viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick) }
        },
        onConfirmDeleteTrustedAppClick = remember(viewModel) {
            {
                viewModel.trySendAction(
                    PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick(it),
                )
            }
        },
    )

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(BitwardenString.privileged_apps),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(PrivilegedAppsListAction.BackClick) }
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
            modifier = Modifier
                .fillMaxSize()
                .standardHorizontalMargin(),
        )
    }
}

@Composable
private fun PrivilegedAppListDialogs(
    state: PrivilegedAppsListState,
    onDismissDialogClick: () -> Unit,
    onConfirmDeleteTrustedAppClick: (app: PrivilegedAppListItem) -> Unit,
) {
    when (val dialogState = state.dialogState) {
        is PrivilegedAppsListState.DialogState.Loading -> {
            BitwardenLoadingDialog(stringResource(BitwardenString.loading))
        }

        is PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp -> {
            BitwardenTwoButtonDialog(
                title = stringResource(BitwardenString.delete),
                message = stringResource(
                    BitwardenString.are_you_sure_you_want_to_stop_trusting_x,
                    dialogState.app.packageName,
                ),
                confirmButtonText = stringResource(BitwardenString.okay),
                dismissButtonText = stringResource(BitwardenString.cancel),
                onConfirmClick = { onConfirmDeleteTrustedAppClick(dialogState.app) },
                onDismissClick = onDismissDialogClick,
                onDismissRequest = onDismissDialogClick,
            )
        }

        is PrivilegedAppsListState.DialogState.General -> {
            BitwardenBasicDialog(
                title = dialogState.title?.invoke()
                    ?: stringResource(BitwardenString.an_error_has_occurred),
                message = dialogState.message.invoke(),
                onDismissRequest = onDismissDialogClick,
            )
        }

        null -> Unit
    }
}

@Suppress("LongMethod")
@Composable
private fun PrivilegedAppsListContent(
    state: PrivilegedAppsListState,
    onDeleteClick: (PrivilegedAppListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAllTrustedApps by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier,
    ) {
        if (state.installedApps.isNotEmpty()) {
            item(key = "installed_apps") {
                Spacer(
                    modifier = Modifier
                        .height(12.dp)
                        .animateItem(),
                )
                BitwardenListHeaderText(
                    label = stringResource(BitwardenString.installed_apps),
                    supportingLabel = state.installedApps.size.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                        .animateItem(),
                )
            }

            itemsIndexed(
                key = { _, item -> "installedApp_$item" },
                items = state.installedApps,
            ) { index, item ->
                BitwardenTextRow(
                    text = item.label,
                    description = item.trustAuthority.description.toAnnotatedString(),
                    clickable = false,
                    onClick = {},
                    cardStyle = state.installedApps
                        .toListItemCardStyle(index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    if (item.canRevokeTrust) {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_delete,
                            contentDescription =
                                stringResource(BitwardenString.delete_x, item.packageName),
                            onClick = remember(item) {
                                { onDeleteClick(item) }
                            },
                        )
                    }
                }
            }
        }

        if (state.notInstalledApps.isNotEmpty()) {
            item {
                BitwardenExpandingHeader(
                    collapsedText = stringResource(BitwardenString.all_trusted_apps),
                    isExpanded = showAllTrustedApps,
                    onClick = remember(state) {
                        { showAllTrustedApps = !showAllTrustedApps }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        if (showAllTrustedApps) {
            if (state.notInstalledUserTrustedApps.isNotEmpty()) {
                item(key = "trusted_by_you") {
                    Spacer(
                        modifier = Modifier
                            .height(12.dp)
                            .animateItem(),
                    )
                    BitwardenListHeaderText(
                        label = stringResource(BitwardenString.trusted_by_you),
                        supportingLabel = state.notInstalledUserTrustedApps.size.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                            .animateItem(),
                    )
                }

                itemsIndexed(
                    key = { _, item -> "userTrust_$item" },
                    items = state.notInstalledUserTrustedApps,
                ) { index, item ->
                    BitwardenTextRow(
                        text = item.label,
                        onClick = {},
                        clickable = false,
                        cardStyle = state.notInstalledUserTrustedApps
                            .toListItemCardStyle(index),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_delete,
                            contentDescription = "",
                            onClick = remember(item) {
                                { onDeleteClick(item) }
                            },
                        )
                    }
                }
            }

            if (state.notInstalledCommunityTrustedApps.isNotEmpty()) {
                item(key = "trusted_by_community") {
                    Spacer(
                        modifier = Modifier
                            .height(
                                if (state.notInstalledUserTrustedApps.isEmpty()) {
                                    12.dp
                                } else {
                                    16.dp
                                },
                            )
                            .animateItem(),
                    )
                    BitwardenListHeaderText(
                        label = stringResource(BitwardenString.trusted_by_the_community),
                        supportingLabel = state.notInstalledCommunityTrustedApps.size.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                            .animateItem(),
                    )
                }

                itemsIndexed(
                    key = { _, item -> "communityTrust_$item" },
                    items = state.notInstalledCommunityTrustedApps,
                ) { index, item ->
                    BitwardenTextRow(
                        text = item.label,
                        onClick = {},
                        clickable = false,
                        cardStyle = state.notInstalledCommunityTrustedApps
                            .toListItemCardStyle(index),
                        modifier = Modifier
                            .animateItem(),
                    )
                }
            }

            if (state.notInstalledGoogleTrustedApps.isNotEmpty()) {
                item(key = "trusted_by_google") {
                    Spacer(modifier = Modifier.height(16.dp))
                    BitwardenListHeaderText(
                        label = stringResource(BitwardenString.trusted_by_google),
                        supportingLabel = state.notInstalledGoogleTrustedApps.size.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                            .animateItem(),
                    )
                }

                itemsIndexed(
                    key = { _, item -> "googleTrust_$item" },
                    items = state.notInstalledGoogleTrustedApps,
                ) { index, item ->
                    BitwardenTextRow(
                        text = item.label,
                        onClick = {},
                        clickable = false,
                        cardStyle = state.notInstalledGoogleTrustedApps
                            .toListItemCardStyle(index),
                        modifier = Modifier
                            .animateItem(),
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
private fun PrivilegedAppsListScreen_Preview() {
    PrivilegedAppsListContent(
        state = PrivilegedAppsListState(
            installedApps = persistentListOf(
                PrivilegedAppListItem(
                    packageName = "com.x8bit.bitwarden.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
                    appName = null,
                ),
                PrivilegedAppListItem(
                    packageName = "com.bitwarden.authenticator.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                    appName = "Bitwarden",
                ),
                PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel.google",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
                    appName = "Bitwarden",
                ),
            ),
            notInstalledApps = persistentListOf(
                PrivilegedAppListItem(
                    packageName = "com.x8bit.bitwarden.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
                    appName = "Bitwarden",
                ),
                PrivilegedAppListItem(
                    packageName = "com.bitwarden.authenticator.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                    appName = "Bitwarden",
                ),
                PrivilegedAppListItem(
                    packageName = "com.google.android.apps.walletnfcrel.community",
                    signature = "XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
                    appName = "Bitwarden",
                ),
            ),
            dialogState = null,
        ),
        onDeleteClick = {},
    )
}
//endregion Previews
