package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R

/**
 * A custom Bitwarden-themed large top app bar with an overflow menu action.
 *
 * This app bar wraps around the Material 3's [LargeTopAppBar] and customizes its appearance
 * and behavior according to the app theme.
 * It provides a title and an optional overflow menu, represented by a dropdown containing
 * a set of menu items.
 *
 * @param title The text to be displayed as the title of the app bar.
 * @param dropdownMenuItemContent A single overflow menu in the right with contents
 *   defined by the [dropdownMenuItemContent]. It is strongly recommended that this content
 *   be a stack of [DropdownMenuItem].
 * @param scrollBehavior Defines the scrolling behavior of the app bar. It controls how the app bar
 * behaves in conjunction with scrolling content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenLargeTopAppBar(
    title: String,
    dropdownMenuItemContent: @Composable ColumnScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var isOverflowMenuVisible by remember { mutableStateOf(false) }

    LargeTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = {
            Box {
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
        },
    )
}
