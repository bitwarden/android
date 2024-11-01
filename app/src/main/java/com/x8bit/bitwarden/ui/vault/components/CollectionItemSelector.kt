package com.x8bit.bitwarden.ui.vault.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

/**
 * A set of switches that a user can select [Collection]s with.
 */
fun LazyListScope.collectionItemsSelector(
    collectionList: List<VaultCollection>?,
    onCollectionSelect: (VaultCollection) -> Unit,
    isCollectionsTitleVisible: Boolean = true,
) {

    if (isCollectionsTitleVisible) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.collections),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }

    if (collectionList?.isNotEmpty() == true) {
        items(collectionList) {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenSwitch(
                label = it.name,
                isChecked = it.isSelected,
                onCheckedChange = { _ ->
                    onCollectionSelect(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("CollectionItemCell")
                    .padding(horizontal = 16.dp),
            )
        }
    } else {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.no_collections_to_list),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
        }
    }
}
