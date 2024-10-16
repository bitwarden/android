package com.x8bit.bitwarden.ui.platform.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable card for displaying content for a list of items with a generic type [T].
 * Items will be displayed in [Column] in the order they are provided with an optional divider
 * below them, besides the last item in the list.
 *
 * @param contentItems list of items to display.
 * @param content composable to render each item to the UI.
 * @param showBottomDivider whether to show a divider below each item.
 * @param bottomDividerPaddingStart padding to apply to the start of the divider.
 * @param bottomDividerPaddingEnd padding to apply to the end of the divider.
 */
@Composable
fun <T> BitwardenContentCard(
    contentItems: ImmutableList<T>,
    modifier: Modifier = Modifier,
    showBottomDivider: Boolean = true,
    bottomDividerPaddingStart: Dp = 0.dp,
    bottomDividerPaddingEnd: Dp = 0.dp,
    content: @Composable (T) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BitwardenTheme.colorScheme.background.secondary,
                shape = BitwardenTheme.shapes.content,
            ),
    ) {
        contentItems.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bottomDivider(
                        enabled = index != contentItems.lastIndex && showBottomDivider,
                        paddingStart = bottomDividerPaddingStart,
                        paddingEnd = bottomDividerPaddingEnd,
                    ),
            ) {
                content(item)
            }
        }
    }
}
