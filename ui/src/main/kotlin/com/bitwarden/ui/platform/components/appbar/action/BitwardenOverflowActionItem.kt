package com.bitwarden.ui.platform.components.appbar.action

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.appbar.color.bitwardenMenuItemColors
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
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
@Composable
fun BitwardenOverflowActionItem(
    menuItemDataList: ImmutableList<OverflowMenuItemData>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    @DrawableRes vectorIconRes: Int = BitwardenDrawable.ic_ellipsis_vertical,
    testTag: String? = "HeaderBarOptionsButton",
) {
    if (menuItemDataList.isEmpty()) return
    var isOverflowMenuVisible by rememberSaveable { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        BitwardenStandardIconButton(
            vectorIconRes = vectorIconRes,
            contentDescription = contentDescription,
            onClick = { isOverflowMenuVisible = !isOverflowMenuVisible },
            modifier = Modifier.nullableTestTag(tag = testTag),
        )
        DropdownMenu(
            shape = BitwardenTheme.shapes.menu,
            expanded = isOverflowMenuVisible,
            onDismissRequest = { isOverflowMenuVisible = false },
            offset = DpOffset(x = (-12).dp, y = 0.dp),
            containerColor = BitwardenTheme.colorScheme.background.primary,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("FloatingOptionsContent")
                .widthIn(
                    min = 112.dp,
                    max = 280.dp,
                ),
            content = {
                menuItemDataList.forEach { dropdownMenuItemData ->
                    DropdownMenuItem(
                        modifier = Modifier.testTag("FloatingOptionsItem"),
                        colors = bitwardenMenuItemColors(
                            textColor = dropdownMenuItemData
                                .color
                                .takeUnless { it == Color.Unspecified }
                                ?: BitwardenTheme.colorScheme.text.primary,
                        ),
                        enabled = dropdownMenuItemData.isEnabled,
                        text = {
                            Text(
                                text = dropdownMenuItemData.text,
                                style = BitwardenTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("FloatingOptionsItemName"),
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
            contentDescription = "More",
            menuItemDataList = persistentListOf(
                OverflowMenuItemData(
                    text = "Test",
                    onClick = {},
                ),
            ),
        )
    }
}
