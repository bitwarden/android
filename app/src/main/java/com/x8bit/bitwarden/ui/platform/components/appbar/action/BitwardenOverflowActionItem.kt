package com.x8bit.bitwarden.ui.platform.components.appbar.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Represents a composable overflow item specifically tailored for Bitwarden's UI.
 *
 * This composable wraps an [IconButton] with an "overflow" icon, typically used to
 * indicate more actions available that are not immediately visible on the interface.
 * The item is centrally aligned within a predefined [Box] of size 24.dp.
 *
 * @param menuItemDataList The list of [OverflowMenuItemData] that will populate the overflow
 * dropdown menu.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BitwardenOverflowActionItem(
    modifier: Modifier = Modifier,
    menuItemDataList: ImmutableList<OverflowMenuItemData> = persistentListOf(),
) {
    if (menuItemDataList.isEmpty()) return
    var isOverflowMenuVisible by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        IconButton(
            onClick = { isOverflowMenuVisible = !isOverflowMenuVisible },
            modifier = Modifier.testTag("HeaderBarOptionsButton"),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(id = R.string.more),
            )
        }
        DropdownMenu(
            expanded = isOverflowMenuVisible,
            onDismissRequest = { isOverflowMenuVisible = false },
            offset = DpOffset(x = (-12).dp, y = 0.dp),
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .widthIn(
                    min = 112.dp,
                    max = 280.dp,
                )
                .background(MaterialTheme.colorScheme.surfaceContainer),
            content = {
                menuItemDataList.forEach { dropdownMenuItemData ->
                    DropdownMenuItem(
                        modifier = Modifier.semantics {
                            dropdownMenuItemData.testTag?.let {
                                testTag = it
                            }
                        },
                        text = {
                            Text(
                                text = dropdownMenuItemData.text,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            isOverflowMenuVisible = false
                            dropdownMenuItemData.onClick()
                        },
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenOverflowActionItem_preview() {
    BitwardenTheme {
        BitwardenOverflowActionItem(
            menuItemDataList = persistentListOf(
                OverflowMenuItemData(
                    text = "Test",
                    onClick = {},
                ),
            ),
        )
    }
}

/**
 * Data used to populate one row of an overflow dropdown menu.
 *
 * @param text The text displayed for the item in the menu.
 * @param onClick A callback for when the menu item is clicked.
 * @param testTag Optional test tag for the menu item.
 */
data class OverflowMenuItemData(
    val text: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)
