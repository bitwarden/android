package com.x8bit.bitwarden.ui.vault.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

/**
 * A set of switches that a user can select [Collection]s with.
 */
fun LazyListScope.collectionItemsSelector(
    collectionList: List<VaultCollection>?,
    onCollectionSelect: (VaultCollection) -> Unit,
    isCollectionsTitleVisible: Boolean = false,
) {
    if (isCollectionsTitleVisible) {
        item {
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.collections),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
    }

    if (collectionList?.isNotEmpty() == true) {
        itemsIndexed(collectionList) { index, it ->
            BitwardenSwitch(
                label = it.name,
                isChecked = it.isSelected,
                onCheckedChange = { _ ->
                    onCollectionSelect(it)
                },
                cardStyle = if (isCollectionsTitleVisible) {
                    // The header is present so display all collections as a single card.
                    collectionList.toListItemCardStyle(index = index)
                } else if (collectionList.size == 1 || index == collectionList.size - 1) {
                    // No header, and this is the last item.
                    CardStyle.Bottom
                } else {
                    // No header, and this is not the last item.
                    CardStyle.Middle()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("CollectionItemCell")
                    .standardHorizontalMargin(),
            )
        }
    } else {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            ) {
                Text(
                    text = stringResource(id = BitwardenString.no_collections_to_list),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
        }
    }
}
