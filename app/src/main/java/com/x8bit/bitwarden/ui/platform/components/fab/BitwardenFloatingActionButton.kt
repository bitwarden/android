package com.x8bit.bitwarden.ui.platform.components.fab

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/**
 * Represents a Bitwarden-styled [FloatingActionButton].
 *
 * @param onClick The callback when the button is clicked.
 * @param painter The icon for the button.
 * @param contentDescription The content description for the button.
 * @param modifier The [Modifier] to be applied to the button.
 */
@Composable
fun BitwardenFloatingActionButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
