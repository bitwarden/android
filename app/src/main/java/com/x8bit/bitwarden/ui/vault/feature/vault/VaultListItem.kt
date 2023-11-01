package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.x8bit.bitwarden.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A composable function that displays a list item.
 * The list item consists of a start icon, a label, a supporting label, and a trailing icon.
 *
 * @param startIcon The [Painter] object used to draw the icon at the start of the list item.
 * @param label The main text label to be displayed in the list item.
 * @param supportingLabel The secondary supporting text label to be displayed beside the main label.
 * @param onClick A lambda function that is invoked when the list item is clicked.
 * @param modifier The [Modifier] to be applied to the [Row] composable that holds the list item.
 */
@Composable
fun VaultListItem(
    startIcon: Painter,
    label: String,
    supportingLabel: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            )
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = startIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = supportingLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_navigate_next),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultListItem_preview() {
    BitwardenTheme {
        VaultListItem(
            startIcon = painterResource(id = R.drawable.ic_login_item),
            label = "Main Text",
            supportingLabel = "100",
            onClick = {},
            modifier = Modifier,
        )
    }
}
