package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultNoItems

/**
 * Empty view for the [VaultItemListingScreen].
 */
@Composable
fun VaultItemListingEmpty(
    paddingValues: PaddingValues,
    itemListingType: VaultItemListingState.ItemListingType,
    addItemClickAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Folder -> {
            GenericNoItems(
                modifier = modifier,
                text = stringResource(id = R.string.no_items_folder),
            )
        }

        is VaultItemListingState.ItemListingType.Trash -> {
            GenericNoItems(
                modifier = modifier,
                text = stringResource(id = R.string.no_items_trash),
            )
        }

        else -> {
            VaultNoItems(
                paddingValues = paddingValues,
                addItemClickAction = addItemClickAction,
            )
        }
    }
}

@Composable
private fun GenericNoItems(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
