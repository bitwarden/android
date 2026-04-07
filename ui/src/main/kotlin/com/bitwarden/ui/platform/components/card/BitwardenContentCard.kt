package com.bitwarden.ui.platform.components.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.bitwarden.ui.platform.base.util.cardBackground
import com.bitwarden.ui.platform.components.content.BitwardenContentBlock
import com.bitwarden.ui.platform.components.content.model.ContentBlockData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable card for displaying content block components in a vertical column with the card
 * shape. Content is drawn with a [BitwardenContentBlock].
 *
 * @param contentItems list of [ContentBlockData] items to display.
 * @param contentHeaderTextStyle the text style to use for the header text of the content.
 * @param contentSubtitleTextStyle the text style to use for the subtitle text of the content.
 * @param contentSubtitleColor the color that should be applied to subtitle text of the content.
 * @param contentBackgroundColor the background color to use for the content.
 */
@Composable
fun BitwardenContentCard(
    contentItems: ImmutableList<ContentBlockData>,
    modifier: Modifier = Modifier,
    contentHeaderTextStyle: TextStyle = BitwardenTheme.typography.titleSmall,
    contentSubtitleTextStyle: TextStyle = BitwardenTheme.typography.bodyMedium,
    contentSubtitleColor: Color = BitwardenTheme.colorScheme.text.secondary,
    contentBackgroundColor: Color = BitwardenTheme.colorScheme.background.secondary,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardBackground(cardStyle = CardStyle.Full, color = contentBackgroundColor),
    ) {
        contentItems.forEachIndexed { index, item ->
            BitwardenContentBlock(
                data = item,
                showDivider = index != contentItems.lastIndex,
                headerTextStyle = contentHeaderTextStyle,
                subtitleTextStyle = contentSubtitleTextStyle,
                subtitleColor = contentSubtitleColor,
                backgroundColor = contentBackgroundColor,
            )
        }
    }
}
