package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Screen for selecting the type of vault item to create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun ItemTypeSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddItem: (VaultItemCipherType) -> Unit,
    viewModel: ItemTypeSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    )

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ItemTypeSelectionEvent.NavigateBack -> onNavigateBack()
            is ItemTypeSelectionEvent.NavigateToAddItem -> {
                onNavigateToAddItem(event.cipherType)
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.new_item),
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(
                        id = BitwardenDrawable.ic_back,
                    ),
                    navigationIconContentDescription = stringResource(
                        id = BitwardenString.back,
                    ),
                    onNavigationIconClick = {
                        viewModel.trySendAction(
                            ItemTypeSelectionAction.BackClick,
                        )
                    },
                ),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ItemTypeSelectionList"),
        ) {
            item(key = "top_spacer") {
                Spacer(modifier = Modifier.height(height = 12.dp))
            }

            itemsIndexed(
                items = state.itemTypes,
                key = { _, option -> option.cipherType.name },
            ) { index, option ->
                BitwardenGroupItem(
                    startIcon = option.icon,
                    label = option.title(),
                    supportingLabel = "",
                    onClick = {
                        viewModel.trySendAction(
                            ItemTypeSelectionAction.ItemTypeClick(
                                cipherType = option.cipherType,
                            ),
                        )
                    },
                    cardStyle = state.itemTypes
                        .toListItemCardStyle(
                            index = index,
                            dividerPadding = 56.dp,
                        ),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .testTag("ItemTypeOption")
                        .standardHorizontalMargin(),
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(height = 24.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
