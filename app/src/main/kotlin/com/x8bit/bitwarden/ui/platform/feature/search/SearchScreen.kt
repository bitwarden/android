package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenSearchTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.RegisterScreenDataOnLifecycleEffect
import com.x8bit.bitwarden.ui.platform.composition.LocalAppResumeStateManager
import com.x8bit.bitwarden.ui.platform.feature.search.handlers.SearchHandlers
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultFilter
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import kotlinx.collections.immutable.toImmutableList

/**
 * The search UI for vault items or send items.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (route: ViewSendRoute) -> Unit,
    onNavigateToEditCipher: (args: VaultAddEditArgs) -> Unit,
    onNavigateToViewCipher: (args: VaultItemArgs) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: SearchViewModel = hiltViewModel(),
    appResumeStateManager: AppResumeStateManager = LocalAppResumeStateManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val searchHandlers = remember(viewModel) { SearchHandlers.create(viewModel) }
    RegisterScreenDataOnLifecycleEffect(
        appResumeStateManager = appResumeStateManager,
    ) {
        AppResumeScreenData.SearchScreen(
            searchTerm = state.searchTerm,
        )
    }

    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SearchEvent.NavigateBack -> onNavigateBack()
            is SearchEvent.NavigateToEditSend -> {
                onNavigateToAddEditSend(
                    AddEditSendRoute(
                        sendType = event.sendType,
                        modeType = ModeType.EDIT,
                        sendId = event.sendId,
                    ),
                )
            }

            is SearchEvent.NavigateToViewSend -> {
                onNavigateToViewSend(
                    ViewSendRoute(sendId = event.sendId, sendType = event.sendType),
                )
            }

            is SearchEvent.NavigateToEditCipher -> {
                onNavigateToEditCipher(
                    VaultAddEditArgs(
                        vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = event.cipherId),
                        vaultItemCipherType = event.cipherType,
                    ),
                )
            }

            is SearchEvent.NavigateToViewCipher -> {
                onNavigateToViewCipher(
                    VaultItemArgs(
                        vaultItemId = event.cipherId,
                        cipherType = event.cipherType,
                    ),
                )
            }

            is SearchEvent.NavigateToUrl -> intentManager.launchUri(event.url.toUri())
            is SearchEvent.ShowShareSheet -> intentManager.shareText(event.content)
            is SearchEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    SearchDialogs(
        dialogState = state.dialogState,
        onDismissRequest = searchHandlers.onDismissRequest,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenSearchTopAppBar(
                searchTerm = state.searchTerm,
                placeholder = state.searchType.title(),
                onSearchTermChange = searchHandlers.onSearchTermChange,
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = searchHandlers.onBackClick,
                ),
                clearIconContentDescription = stringResource(id = BitwardenString.clear),
            )
        },
        utilityBar = {
            val vaultFilterData = state.vaultFilterData
            if (state.viewState.hasVaultFilter && vaultFilterData != null) {
                VaultFilter(
                    selectedVaultFilterType = vaultFilterData.selectedVaultFilterType,
                    vaultFilterTypes = vaultFilterData.vaultFilterTypes.toImmutableList(),
                    onVaultFilterTypeSelect = searchHandlers.onVaultFilterSelect,
                    topAppBarScrollBehavior = scrollBehavior,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            // There is some built-in padding to the menu button that makes up
                            // the visual difference here.
                            end = 12.dp,
                        )
                        .fillMaxWidth(),
                )
            }
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        when (val viewState = state.viewState) {
            is SearchState.ViewState.Content -> SearchContent(
                viewState = viewState,
                searchHandlers = searchHandlers,
                searchType = state.searchType,
                modifier = Modifier
                    .fillMaxSize(),
            )

            is SearchState.ViewState.Empty -> SearchEmptyContent(
                viewState = viewState,
                modifier = Modifier
                    .fillMaxSize(),
            )

            is SearchState.ViewState.Error -> BitwardenErrorContent(
                message = viewState.message(),
                modifier = Modifier
                    .fillMaxSize(),
            )

            SearchState.ViewState.Loading -> BitwardenLoadingContent(
                modifier = Modifier
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SearchDialogs(
    dialogState: SearchState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is SearchState.DialogState.Error -> BitwardenBasicDialog(
            title = dialogState.title?.invoke(),
            message = dialogState.message(),
            onDismissRequest = onDismissRequest,
            throwable = dialogState.throwable,
        )

        is SearchState.DialogState.Loading -> BitwardenLoadingDialog(
            text = dialogState.message(),
        )

        null -> Unit
    }
}
