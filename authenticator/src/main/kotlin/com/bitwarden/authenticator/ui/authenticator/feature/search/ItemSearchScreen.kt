package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.bottomDivider
import com.bitwarden.ui.platform.components.appbar.BitwardenSearchTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The search screen for authenticator items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSearchScreen(
    viewModel: ItemSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val searchHandlers = remember(viewModel) { SearchHandlers.create(viewModel) }
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ItemSearchEvent.NavigateBack -> onNavigateBack()
            is ItemSearchEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
            is ItemSearchEvent.NavigateToEditItem -> onNavigateToEdit(event.itemId)
        }
    }

    ItemSearchDialogs(
        dialogState = state.dialog,
        searchHandlers = searchHandlers,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenSearchTopAppBar(
                modifier = Modifier
                    .testTag("SearchFieldEntry")
                    .bottomDivider(),
                searchTerm = state.searchTerm,
                placeholder = stringResource(id = BitwardenString.search_codes),
                onSearchTermChange = searchHandlers.onSearchTermChange,
                scrollBehavior = scrollBehavior,
                clearIconContentDescription = stringResource(id = BitwardenString.clear),
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = searchHandlers.onBackClick,
                ),
            )
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        when (val viewState = state.viewState) {
            is ItemSearchState.ViewState.Content -> {
                ItemSearchContent(
                    viewState = viewState,
                    searchHandlers = searchHandlers,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is ItemSearchState.ViewState.Empty -> {
                ItemSearchEmptyContent(
                    viewState = viewState,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ItemSearchDialogs(
    dialogState: ItemSearchState.DialogState?,
    searchHandlers: SearchHandlers,
) {
    when (dialogState) {
        is ItemSearchState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                throwable = dialogState.throwable,
                onDismissRequest = searchHandlers.onDismissDialog,
            )
        }

        is ItemSearchState.DialogState.DeleteConfirmationPrompt -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.delete),
                message = dialogState.message(),
                confirmButtonText = stringResource(id = BitwardenString.okay),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = { searchHandlers.onConfirmDeleteClick(dialogState.itemId) },
                onDismissClick = searchHandlers.onDismissDialog,
                onDismissRequest = searchHandlers.onDismissDialog,
            )
        }

        ItemSearchState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = stringResource(id = BitwardenString.loading))
        }

        null -> Unit
    }
}
