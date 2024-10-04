package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.fab.BitwardenFloatingActionButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderDisplayItem
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
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
    val context = LocalContext.current
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is FoldersEvent.NavigateBack -> onNavigateBack()
            is FoldersEvent.NavigateToAddFolderScreen -> onNavigateToAddFolderScreen()
            is FoldersEvent.NavigateToEditFolderScreen ->
                onNavigateToEditFolderScreen(event.folderId)

            is FoldersEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.folders),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
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
                painter = rememberVectorPainter(id = R.drawable.ic_plus),
                contentDescription = stringResource(id = R.string.add_item),
                modifier = Modifier
                    .testTag(tag = "AddItemButton")
                    .navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        when (val viewState = state.value.viewState) {
            is FoldersState.ViewState.Content -> {
                FoldersContent(
                    foldersList = viewState.folderList.toImmutableList(),
                    onItemClick = remember(viewModel) {
                        { viewModel.trySendAction(FoldersAction.FolderClick(it)) }
                    },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            is FoldersState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            is FoldersState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
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
                text = stringResource(id = R.string.no_folders_to_list),
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
            items(foldersList) {
                Row(
                    modifier = Modifier
                        .testTag("FolderCell")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                color = BitwardenTheme.colorScheme.background.pressed,
                            ),
                            onClick = { onItemClick(it.id) },
                        )
                        .bottomDivider(paddingStart = 16.dp)
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .testTag("FolderName")
                            .padding(start = 16.dp)
                            .weight(1f),
                        text = it.name,
                        style = BitwardenTheme.typography.bodyLarge,
                        color = BitwardenTheme.colorScheme.text.primary,
                    )
                }
            }
        }
    }
}
