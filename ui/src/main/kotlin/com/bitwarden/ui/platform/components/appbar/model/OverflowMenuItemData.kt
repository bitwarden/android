package com.bitwarden.ui.platform.components.appbar.model

import androidx.compose.ui.graphics.Color

/**
 * Data used to populate one row of an overflow dropdown menu.
 *
 * @param text The text displayed for the item in the menu.
 * @param isExternalLink Indicates that this item will launch an external link.
 * @param onClick A callback for when the menu item is clicked.
 * @param isEnabled Indicates that this overflow item is enabled or not.
 * @param color The color of the content.
 */
data class OverflowMenuItemData(
    val text: String,
    val onClick: () -> Unit,
    val isExternalLink: Boolean = false,
    val isEnabled: Boolean = true,
    val color: Color = Color.Unspecified,
)
