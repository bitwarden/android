package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a composable overflow item specifically tailored for Bitwarden's UI.
 *
 * This composable wraps an [IconButton] with an "overflow" icon, typically used to
 * indicate more actions available that are not immediately visible on the interface.
 * The item is centrally aligned within a predefined [Box] of size 24.dp.
 *
 * @param dropdownMenuItemContent A single overflow menu in the right with contents
 *   defined by the [dropdownMenuItemContent]. It is strongly recommended that this content
 *   be a stack of [DropdownMenuItem].
 */
@Composable
fun BitwardenOverflowActionItem(
    dropdownMenuItemContent: @Composable ColumnScope.() -> Unit = {},
) {
    var isOverflowMenuVisible by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = { isOverflowMenuVisible = !isOverflowMenuVisible }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = isOverflowMenuVisible,
            onDismissRequest = { isOverflowMenuVisible = false },
            content = dropdownMenuItemContent,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenOverflowActionItem_preview() {
    BitwardenTheme {
        BitwardenOverflowActionItem(dropdownMenuItemContent = {})
    }
}
