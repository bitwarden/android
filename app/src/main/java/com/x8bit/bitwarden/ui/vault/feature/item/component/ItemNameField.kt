package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle

/**
 * Reusable composable for displaying the cipher name and favorite status.
 */
@Composable
fun ItemNameField(
    value: String,
    isFavorite: Boolean,
    textFieldTestTag: String,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.item_name_required),
        value = value,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            Icon(
                painter = painterResource(
                    id = if (isFavorite) {
                        R.drawable.ic_favorite_full
                    } else {
                        R.drawable.ic_favorite_empty
                    },
                ),
                contentDescription = stringResource(
                    id = if (isFavorite) R.string.favorite else R.string.unfavorite,
                ),
                modifier = Modifier.padding(all = 12.dp),
            )
        },
        textFieldTestTag = textFieldTestTag,
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
