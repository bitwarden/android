package com.x8bit.bitwarden.ui.platform.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.card.color.bitwardenCardColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A reusable card for displaying actions to the user.
 */
@Composable
fun BitwardenActionCardSmall(
    actionIcon: VectorPainter,
    actionText: String,
    callToActionText: String,
    callToActionTextColor: Color = BitwardenTheme.colorScheme.text.primary,
    colors: CardColors = bitwardenCardColors(),
    onCardClicked: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Card(
        onClick = onCardClicked,
        shape = BitwardenTheme.shapes.actionCard,
        modifier = modifier,
        colors = colors,
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 1.dp, color = BitwardenTheme.colorScheme.stroke.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(
                painter = actionIcon,
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.secondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(weight = 1f),
            ) {
                Text(
                    text = actionText,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = callToActionText,
                    style = BitwardenTheme.typography.labelLarge,
                    color = callToActionTextColor,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            ) {
                trailingContent?.invoke(this)
            }
        }
    }
}

@Preview
@Composable
private fun ActionCardSmall_preview() {
    BitwardenTheme {
        BitwardenActionCardSmall(
            actionIcon = rememberVectorPainter(id = R.drawable.ic_generate),
            actionText = "This is an action.",
            callToActionText = "Take action",
            onCardClicked = { },
        )
    }
}

@Preview
@Composable
private fun ActionCardSmallWithTrailingIcon_preview() {
    BitwardenTheme {
        BitwardenActionCardSmall(
            actionIcon = rememberVectorPainter(id = R.drawable.ic_generate),
            actionText = "An action with trailing content",
            callToActionText = "Take action",
            onCardClicked = {},
            trailingContent = {
                Icon(
                    painter = rememberVectorPainter(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = BitwardenTheme.colorScheme.icon.primary,
                )
            },
        )
    }
}
