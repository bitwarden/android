package com.x8bit.bitwarden.ui.vault.feature.verificationcode

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.RegisterScreenDataOnLifecycleEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.BitwardenSearchActionItem
import com.x8bit.bitwarden.ui.platform.components.appbar.action.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.composition.LocalAppResumeStateManager
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.handlers.VerificationCodeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the verification codes to the user.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCodeScreen(
    viewModel: VerificationCodeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    appResumeStateManager: AppResumeStateManager = LocalAppResumeStateManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val verificationCodeHandler = remember(viewModel) {
        VerificationCodeHandlers.create(viewModel)
    }

    val pullToRefreshState = rememberBitwardenPullToRefreshState(
        isEnabled = state.isPullToRefreshEnabled,
        isRefreshing = state.isRefreshing,
        onRefresh = remember(viewModel) {
            { viewModel.trySendAction(VerificationCodeAction.RefreshPull) }
        },
    )

    RegisterScreenDataOnLifecycleEffect(
        appResumeStateManager = appResumeStateManager,
    ) {
        AppResumeScreenData.VerificationCodeScreen
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VerificationCodeEvent.NavigateBack -> onNavigateBack()
            is VerificationCodeEvent.NavigateToVaultItem -> {
                onNavigateToVaultItemScreen(VaultItemArgs(event.id, VaultItemCipherType.LOGIN))
            }

            is VerificationCodeEvent.NavigateToVaultSearchScreen -> {
                onNavigateToSearch()
            }
        }
    }

    VerificationCodeDialogs(dialogState = state.dialogState)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.verification_codes),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = verificationCodeHandler.backClick,
                actions = {
                    BitwardenSearchActionItem(
                        contentDescription = stringResource(id = R.string.search_vault),
                        onClick = verificationCodeHandler.searchIconClick,
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOf(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.sync),
                                onClick = verificationCodeHandler.syncClick,
                            ),
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.lock),
                                onClick = verificationCodeHandler.lockClick,
                            ),
                        ),
                    )
                },
            )
        },
        pullToRefreshState = pullToRefreshState,
    ) {
        when (val viewState = state.viewState) {
            is VerificationCodeState.ViewState.Content -> {
                VerificationCodeContent(
                    items = viewState.verificationCodeDisplayItems.toImmutableList(),
                    onCopyClick = verificationCodeHandler.copyClick,
                    itemClick = verificationCodeHandler.itemClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VerificationCodeState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message.invoke(),
                    onTryAgainClick = verificationCodeHandler.refreshClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is VerificationCodeState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun VerificationCodeDialogs(
    dialogState: VerificationCodeState.DialogState?,
) {
    when (dialogState) {
        is VerificationCodeState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}

@Composable
private fun VerificationCodeContent(
    items: ImmutableList<VerificationCodeDisplayItem>,
    itemClick: (id: String) -> Unit,
    onCopyClick: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.items),
                supportingLabel = items.size.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        itemsIndexed(items) { index, it ->
            VaultVerificationCodeItem(
                startIcon = it.startIcon,
                label = it.label,
                supportingLabel = it.supportingLabel,
                timeLeftSeconds = it.timeLeftSeconds,
                periodSeconds = it.periodSeconds,
                authCode = it.authCode,
                hideAuthCode = it.hideAuthCode,
                onCopyClick = { onCopyClick(it.authCode) },
                onItemClick = {
                    itemClick(it.id)
                },
                cardStyle = items.toListItemCardStyle(index = index, dividerPadding = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
