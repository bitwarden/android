package com.bitwarden.authenticator.ui.platform.components.card

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter

/**
 * A reusable card for displaying actions to the user.
 */
@Composable
fun BitwardenActionCard(
    actionIcon: VectorPainter,
    titleText: String,
    actionText: String,
    callToActionText: String,
    onCardClicked: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Card(
        onClick = onCardClicked,
        shape = RoundedCornerShape(size = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Icon(
                painter = actionIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = callToActionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box {
                trailingContent?.invoke(this)
            }
        }
    }
}

@Preview
@Composable
private fun ActionCardPreview() {
    BitwardenActionCard(
        actionIcon = rememberVectorPainter(id = R.drawable.ic_close),
        actionText = "This is an action.",
        callToActionText = "Take action",
        titleText = "This is a title",
        onCardClicked = { },
    )
}

@Preview
@Composable
private fun ActionCardWithTrailingPreview() {
    BitwardenActionCard(
        actionIcon = rememberVectorPainter(id = R.drawable.ic_bitwarden),
        actionText = "An action with trailing content",
        titleText = "This is a title",
        callToActionText = "Take action",
        onCardClicked = {},
        trailingContent = {
            IconButton(
                onClick = {},
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp),
                )
            }
        },
    )
}
