package com.bitwarden.ui.platform.components.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardBackground
import com.bitwarden.ui.platform.base.util.nullableClickable
import com.bitwarden.ui.platform.components.model.CardStyle

/**
 * Component for displaying supporting content within a card, usually at the bottom.
 *
 * @param cardStyle The card style to be applied.
 * @param modifier The modifier to be applied.
 * @param insets Insets to be applied to within the card. This value will be applied even if the
 * [CardStyle] is null.
 * @param onClick The callback to be invoked when the supporting content is clicked.
 * @param content The content to be displayed within the card.
 */
@Composable
fun BitwardenSupportingContent(
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    insets: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 12.dp,
        bottom = 12.dp,
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        content = content,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .cardBackground(cardStyle = cardStyle)
            .nullableClickable(onClick = onClick)
            .padding(paddingValues = insets),
    )
}
