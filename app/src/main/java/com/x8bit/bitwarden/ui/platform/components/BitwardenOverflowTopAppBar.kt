package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled [TopAppBar] that assumes the following components:
 *
 * - a single navigation control in the upper-left defined by [navigationIcon],
 *   [navigationIconContentDescription], and [onNavigationIconClick].
 * - a [title] in the middle.
 * - a single overflow menu in the right with contents defined by the [dropdownMenuItemContent]. It
 *   is strongly recommended that this content be a stack of [DropdownMenuItem].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenOverflowTopAppBar(
    title: String,
    navigationIcon: Painter,
    navigationIconContentDescription: String,
    onNavigationIconClick: () -> Unit,
    dropdownMenuItemContent: @Composable ColumnScope.() -> Unit,
) {
    var isOverflowMenuVisible by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = { onNavigationIconClick() },
            ) {
                Icon(
                    painter = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = {
            Box {
                IconButton(
                    onClick = { isOverflowMenuVisible = !isOverflowMenuVisible },
                ) {
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
        },
    )
}

@Preview
@Composable
private fun BitwardenOverflowTopAppBar_preview() {
    BitwardenTheme {
        BitwardenOverflowTopAppBar(
            title = "Title",
            navigationIcon = painterResource(id = R.drawable.ic_close),
            navigationIconContentDescription = stringResource(id = R.string.close),
            onNavigationIconClick = {},
            dropdownMenuItemContent = {},
        )
    }
}
