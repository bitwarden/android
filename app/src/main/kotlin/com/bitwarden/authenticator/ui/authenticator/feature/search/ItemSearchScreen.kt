package com.bitwarden.authenticator.ui.authenticator.feature.search

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.bottomDivider
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenSearchTopAppBar
import com.bitwarden.authenticator.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.authenticator.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.authenticator.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold

/**
 * The search screen for authenticator items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSearchScreen(
    viewModel: ItemSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val searchHandlers = remember(viewModel) { SearchHandlers.create(viewModel) }
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ItemSearchEvent.NavigateBack -> onNavigateBack()
            is ItemSearchEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    ItemSearchDialogs(
        dialogState = state.dialogState,
        onDismissRequest = searchHandlers.onDismissRequest,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenSearchTopAppBar(
                modifier = Modifier
                    .semantics { testTag = "SearchFieldEntry" }
                    .bottomDivider(),
                searchTerm = state.searchTerm,
                placeholder = stringResource(id = R.string.search_codes),
                onSearchTermChange = searchHandlers.onSearchTermChange,
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = R.drawable.ic_back),
                    navigationIconContentDescription = stringResource(id = R.string.back),
                    onNavigationIconClick = searchHandlers.onBackClick,
                ),
            )
        },
    )
    { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val innerModifier = Modifier
                .fillMaxSize()
                .imePadding()

            when (val viewState = state.viewState) {
                is ItemSearchState.ViewState.Content -> {
                    ItemSearchContent(
                        viewState = viewState,
                        searchHandlers = searchHandlers,
                        modifier = innerModifier,
                    )
                }

                is ItemSearchState.ViewState.Empty -> {
                    ItemSearchEmptyContent(
                        viewState = viewState,
                        modifier = innerModifier
                    )
                }

                is ItemSearchState.ViewState.Error -> {
                    BitwardenErrorContent(
                        message = viewState.message(),
                        modifier = innerModifier,
                    )
                }

                is ItemSearchState.ViewState.Loading -> {
                    BitwardenLoadingContent(
                        modifier = innerModifier,
                    )
                }
            }
        }
    }
}

/**
 * Dialogs displayed within the context of the item search screen.
 */
@Composable
private fun ItemSearchDialogs(
    dialogState: ItemSearchState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is ItemSearchState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is ItemSearchState.DialogState.Loading -> {
            BitwardenLoadingDialog(visibilityState = LoadingDialogState.Shown(dialogState.message))
        }

        null -> Unit
    }
}
