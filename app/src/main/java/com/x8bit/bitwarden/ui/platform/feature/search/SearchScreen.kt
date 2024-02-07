package com.x8bit.bitwarden.ui.platform.feature.search

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSearchTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.NavigationIcon
import com.x8bit.bitwarden.ui.platform.feature.search.handlers.SearchHandlers
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultFilter
import kotlinx.collections.immutable.toImmutableList

/**
 * The search UI for vault items or send items.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditSend: (sendId: String) -> Unit,
    onNavigateToEditCipher: (cipherId: String) -> Unit,
    onNavigateToViewCipher: (cipherId: String) -> Unit,
    intentManager: IntentManager = LocalIntentManager.current,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val searchHandlers = remember(viewModel) { SearchHandlers.create(viewModel) }
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            SearchEvent.NavigateBack -> onNavigateBack()
            is SearchEvent.NavigateToEditSend -> onNavigateToEditSend(event.sendId)
            is SearchEvent.NavigateToEditCipher -> onNavigateToEditCipher(event.cipherId)
            is SearchEvent.NavigateToViewCipher -> onNavigateToViewCipher(event.cipherId)
            is SearchEvent.NavigateToUrl -> intentManager.launchUri(event.url.toUri())
            is SearchEvent.ShowShareSheet -> intentManager.shareText(event.content)
            is SearchEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }
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
                    navigationIcon = painterResource(id = R.drawable.ic_back),
                    navigationIconContentDescription = stringResource(id = R.string.back),
                    onNavigationIconClick = searchHandlers.onBackClick,
                ),
                modifier = Modifier
                    .semantics { testTag = "SearchFieldEntry" }
                    .bottomDivider(),
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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

            val innerModifier = Modifier
                .fillMaxSize()
                .imePadding()
            when (val viewState = state.viewState) {
                is SearchState.ViewState.Content -> SearchContent(
                    viewState = viewState,
                    searchHandlers = searchHandlers,
                    modifier = innerModifier,
                )

                is SearchState.ViewState.Empty -> SearchEmptyContent(
                    viewState = viewState,
                    modifier = innerModifier,
                )

                is SearchState.ViewState.Error -> BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = innerModifier,
                )

                SearchState.ViewState.Loading -> BitwardenLoadingContent(
                    modifier = innerModifier,
                )
            }
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
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is SearchState.DialogState.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(dialogState.message),
        )

        null -> Unit
    }
}
