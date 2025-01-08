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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
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
            Spacer(modifier = Modifier.height(height = 16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.collections),
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
                cardStyle = collectionList.toListItemCardStyle(index = index),
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
                    text = stringResource(id = R.string.no_collections_to_list),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
        }
    }
}
