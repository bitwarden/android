package com.bitwarden.ui.platform.components.appbar.model

import androidx.compose.ui.graphics.Color

/**
 * Data used to populate one row of an overflow dropdown menu.
 *
 * @param text The text displayed for the item in the menu.
 * @param onClick A callback for when the menu item is clicked.
 * @param isEnabled Indicates that this overflow item is enabled or not.
 * @param color The color of the content.
 */
data class OverflowMenuItemData(
    val text: String,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
    val color: Color = Color.Unspecified,
)
