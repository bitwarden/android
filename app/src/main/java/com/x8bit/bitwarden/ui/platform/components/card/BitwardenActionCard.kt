package com.x8bit.bitwarden.ui.platform.components.card

import android.content.res.Configuration
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.badge.NotificationBadge
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.card.color.bitwardenCardColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A design component action card, which contains a title, action button, and a dismiss button
 * by default, with optional leading icon content.
 *
 * @param cardTitle The title of the card.
 * @param actionText The text content on the CTA button.
 * @param onActionClick The action to perform when the CTA button is clicked.
 * @param onDismissClick The action to perform when the dismiss button is clicked.
 * @param leadingContent Optional content to display on the leading side of the
 * [cardTitle] [Text].
 */
@Composable
fun BitwardenActionCard(
    cardTitle: String,
    actionText: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSubtitle: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = BitwardenTheme.shapes.actionCard,
        colors = bitwardenCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 1.dp, color = BitwardenTheme.colorScheme.stroke.border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingContent?.let {
                    it()
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = cardTitle,
                    style = BitwardenTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                BitwardenStandardIconButton(
                    painter = rememberVectorPainter(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.close),
                    onClick = onDismissClick,
                    modifier = Modifier.offset(x = 8.dp),
                )
            }
            cardSubtitle?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            BitwardenFilledButton(
                label = actionText,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A default exit animation for [BitwardenActionCard] when using an animation wrapper like
 * [AnimatedVisibility].
 */
fun actionCardExitAnimation() = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BitwardenActionCard_preview() {
    BitwardenTheme {
        BitwardenActionCard(
            cardTitle = "Title",
            actionText = "Action",
            onActionClick = {},
            onDismissClick = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BitwardenActionCardWithLeadingContent_preview() {
    BitwardenTheme {
        BitwardenActionCard(
            cardTitle = "Title",
            actionText = "Action",
            onActionClick = {},
            onDismissClick = {},
            leadingContent = {
                NotificationBadge(
                    notificationCount = 1,
                )
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BitwardenActionCardWithSubtitle_preview() {
    BitwardenTheme {
        BitwardenActionCard(
            cardTitle = "Title",
            cardSubtitle = "Subtitle",
            actionText = "Action",
            onActionClick = {},
            onDismissClick = {},
            leadingContent = {
                NotificationBadge(
                    notificationCount = 1,
                )
            },
        )
    }
}
