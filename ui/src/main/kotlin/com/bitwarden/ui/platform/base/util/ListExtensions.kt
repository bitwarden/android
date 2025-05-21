package com.bitwarden.ui.platform.base.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.model.CardStyle

/**
 * Returns null if all entries in a given list are equal to a provided [value], otherwise
 * the original list is returned.
 */
fun <T> List<T>.nullIfAllEqual(value: T): List<T>? =
    if (all { it == value }) {
        null
    } else {
        this
    }

/**
 * Returns the appropriate [CardStyle] based on the current [index] in the list.
 */
fun <T> Collection<T>.toListItemCardStyle(
    index: Int,
    hasDivider: Boolean = true,
    dividerPadding: Dp = 16.dp,
): CardStyle =
    if (this.size == 1) {
        CardStyle.Full
    } else if (index == 0) {
        CardStyle.Top(hasDivider = hasDivider, dividerPadding = dividerPadding)
    } else if (index == this.size - 1) {
        CardStyle.Bottom
    } else {
        CardStyle.Middle(hasDivider = hasDivider, dividerPadding = dividerPadding)
    }
