package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.components.collectionItemsSelector
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import kotlinx.collections.immutable.toImmutableList

/**
 * Content view for the [VaultMoveToOrganizationScreen].
 */
@Suppress("LongMethod")
@Composable
fun VaultMoveToOrganizationContent(
    state: VaultMoveToOrganizationState.ViewState.Content,
    showOnlyCollections: Boolean,
    organizationSelect: (VaultMoveToOrganizationState.ViewState.Content.Organization) -> Unit,
    collectionSelect: (VaultCollection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("CollectionListContainer"),
    ) {
        if (!showOnlyCollections) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenMultiSelectButton(
                    label = stringResource(id = R.string.organization),
                    options = state
                        .organizations
                        .map { it.name }
                        .toImmutableList(),
                    selectedOption = state.selectedOrganization.name,
                    onOptionSelected = { selectedString ->
                        organizationSelect(
                            state
                                .organizations
                                .first { it.name == selectedString },
                        )
                    },
                    modifier = Modifier
                        .testTag("OrganizationListDropdown")
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.move_to_org_desc),
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                }
            }
        }

        collectionItemsSelector(
            collectionList = state.selectedOrganization.collections,
            onCollectionSelect = collectionSelect,
            isCollectionsTitleVisible = !showOnlyCollections,
        )
    }
}
