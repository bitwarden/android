package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import kotlinx.collections.immutable.toImmutableList

/**
 * Content view for the [VaultMoveToOrganizationScreen].
 */
@Suppress("LongMethod")
@Composable
fun VaultMoveToOrganizationContent(
    state: VaultMoveToOrganizationState.ViewState.Content,
    organizationSelect: (VaultMoveToOrganizationState.ViewState.Content.Organization) -> Unit,
    collectionSelect: (VaultMoveToOrganizationState.ViewState.Content.Collection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
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
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.move_to_org_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.collections),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        if (state.selectedOrganization.collections.isNotEmpty()) {
            items(state.selectedOrganization.collections) {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenWideSwitch(
                    label = it.name,
                    isChecked = it.isSelected,
                    onCheckedChange = { _ ->
                        collectionSelect(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.no_collections_to_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
