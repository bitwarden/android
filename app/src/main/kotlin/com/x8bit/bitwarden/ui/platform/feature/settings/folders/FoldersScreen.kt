package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderDisplayItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the folders screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddFolderScreen: () -> Unit,
    onNavigateToEditFolderScreen: (folderId: String) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is FoldersEvent.NavigateBack -> onNavigateBack()
            is FoldersEvent.NavigateToAddFolderScreen -> onNavigateToAddFolderScreen()
            is FoldersEvent.NavigateToEditFolderScreen -> {
                onNavigateToEditFolderScreen(event.folderId)
            }

            is FoldersEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.folders),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(FoldersAction.CloseButtonClick) }
                },
            )
        },
        floatingActionButton = {
            BitwardenFloatingActionButton(
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(FoldersAction.AddFolderButtonClick) }
                },
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_plus_large),
                contentDescription = stringResource(id = BitwardenString.add_item),
                modifier = Modifier
                    .testTag(tag = "AddItemButton")
                    .navigationBarsPadding(),
            )
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        when (val viewState = state.value.viewState) {
            is FoldersState.ViewState.Content -> {
                FoldersContent(
                    foldersList = viewState.folderList.toImmutableList(),
                    onItemClick = remember(viewModel) {
                        { viewModel.trySendAction(FoldersAction.FolderClick(it)) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is FoldersState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is FoldersState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FoldersContent(
    foldersList: ImmutableList<FolderDisplayItem>,
    onItemClick: (folderId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (foldersList.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = BitwardenString.no_folders_to_list),
                textAlign = TextAlign.Center,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.testTag("NoFoldersLabel"),
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
        ) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
            }
            itemsIndexed(foldersList) { index, it ->
                BitwardenTextRow(
                    text = it.name,
                    onClick = { onItemClick(it.id) },
                    textTestTag = "FolderName",
                    cardStyle = foldersList.toListItemCardStyle(index = index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .testTag(tag = "FolderCell"),
                )
            }
            item {
                Spacer(modifier = Modifier.height(height = 88.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
