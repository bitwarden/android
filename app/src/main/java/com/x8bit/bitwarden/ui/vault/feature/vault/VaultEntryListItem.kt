package com.x8bit.bitwarden.ui.vault.feature.vault

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.SelectionItemData
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * A Composable function that displays a row item for different types of vault entries.
 *
 * @param label The primary text label to display for the item.
 * @param supportingLabel An optional secondary text label to display beneath the primary label.
 * @param onClick The lambda to be invoked when the item is clicked.
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 */
@Composable
fun VaultEntryListItem(
    startIcon: IconData,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
) {
    val context = LocalContext.current
    BitwardenListItem(
        modifier = modifier,
        label = label,
        supportingLabel = supportingLabel,
        startIcon = startIcon,
        onClick = onClick,
        selectionDataList = persistentListOf(
            SelectionItemData(
                text = "Not yet implemented",
                onClick = {
                    // TODO: Provide dialog-based implementation (BIT-1353 - BIT-1356)
                    Toast.makeText(context, "Not yet implemented.", Toast.LENGTH_SHORT).show()
                },
            ),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun VaultEntryListItem_preview() {
    BitwardenTheme {
        VaultEntryListItem(
            startIcon = IconData.Local(R.drawable.ic_login_item),
            label = "Example Login",
            supportingLabel = "Username",
            onClick = {},
            modifier = Modifier,
        )
    }
}
