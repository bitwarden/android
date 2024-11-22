package com.x8bit.bitwarden.ui.platform.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenContentBlock
import com.x8bit.bitwarden.ui.platform.components.model.ContentBlockData
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable card for displaying content block components in a vertical column with the card
 * shape. Content is drawn with a [BitwardenContentBlock].
 *
 * @param contentItems list of [ContentBlockData] items to display.
 * @param contentHeaderTextStyle the text style to use for the header text of the content.
 * @param contentSubtitleTextStyle the text style to use for the subtitle text of the content.
 * @param contentBackgroundColor the background color to use for the content.
 */
@Composable
fun BitwardenContentCard(
    contentItems: ImmutableList<ContentBlockData>,
    modifier: Modifier = Modifier,
    contentHeaderTextStyle: TextStyle = BitwardenTheme.typography.titleSmall,
    contentSubtitleTextStyle: TextStyle = BitwardenTheme.typography.bodyMedium,
    contentBackgroundColor: Color = BitwardenTheme.colorScheme.background.secondary,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = BitwardenTheme.shapes.content)
            .background(color = BitwardenTheme.colorScheme.background.secondary),
    ) {
        contentItems.forEachIndexed { index, item ->
            BitwardenContentBlock(
                data = item,
                showDivider = index != contentItems.lastIndex,
                headerTextStyle = contentHeaderTextStyle,
                subtitleTextStyle = contentSubtitleTextStyle,
                backgroundColor = contentBackgroundColor,
            )
        }
    }
}
