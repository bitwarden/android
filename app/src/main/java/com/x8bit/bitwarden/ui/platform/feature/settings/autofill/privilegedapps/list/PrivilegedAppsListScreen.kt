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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model.PrivilegedAppListItem
import kotlinx.collections.immutable.toImmutableList

/**
 * Top level composable for the privileged apps list.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivilegedAppsListScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrivilegedAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    EventsEffect(viewModel) { event ->
        when (event) {
            PrivilegedAppsListEvent.NavigateBack -> onNavigateBack()
        }
    }

    when (val dialogState = state.dialogState) {
        is Fido2TrustState.DialogState.Loading -> {
            BitwardenLoadingDialog(stringResource(R.string.loading))
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
                BitwardenListHeaderText(
                    label = stringResource(R.string.trusted_by_you),
                    supportingLabel = state.userTrustedApps.size.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(
                key = { _, item -> "userTrust_${item.packageName}_${item.signature}" },
                items = state.userTrustedApps,
            ) { index, item ->
                BitwardenTextRow(
                    text = item.packageName,
                    onClick = {},
                    cardStyle = state.userTrustedApps
                        .toListItemCardStyle(index),
                    description = item.signature,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .animateItem(),
                ) {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_delete,
                        contentDescription = "",
                        onClick = remember(item) {
                            { onDeleteClick(item) }
                        },
                    )
                }
            }
        }

        if (state.communityTrustedApps.isNotEmpty()) {
            item(key = "trusted_by_community") {
                Spacer(
                    modifier = Modifier
                        .height(if (state.userTrustedApps.isEmpty()) 12.dp else 16.dp)
                        .animateItem(),
                )
                BitwardenListHeaderText(
                    label = stringResource(R.string.trusted_by_the_community),
                    supportingLabel = state.communityTrustedApps.size.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
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
                key = { _, item -> "communityTrust_${item.packageName}_${item.signature}" },
                items = state.communityTrustedApps,
            ) { index, item ->
                BitwardenTextRow(
                    text = item.packageName,
                    onClick = {},
                    cardStyle = state.communityTrustedApps
                        .toListItemCardStyle(index),
                    description = item.signature,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .animateItem(),
                )
            }
        }

        if (state.googleTrustedApps.isNotEmpty()) {
            item(key = "trusted_by_google") {
                Spacer(modifier = Modifier.height(16.dp))
                BitwardenListHeaderText(
                    label = stringResource(R.string.trusted_by_google),
                    supportingLabel = state.googleTrustedApps.size.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
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
                key = { _, item -> "googleTrust_${item.packageName}_${item.signature}" },
                items = state.googleTrustedApps,
            ) { index, item ->
                BitwardenTextRow(
                    text = item.packageName,
                    onClick = {},
                    cardStyle = state.googleTrustedApps
                        .toListItemCardStyle(index),
                    description = item.signature,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .animateItem(),
                )
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
//endregion Previews
