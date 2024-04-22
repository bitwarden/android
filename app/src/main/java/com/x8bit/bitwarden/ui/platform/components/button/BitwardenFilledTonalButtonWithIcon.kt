package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A filled tonal button for the Bitwarden UI with a customized appearance and an icon.
 *
 * This button uses the `secondaryContainer` color from the current [MaterialTheme.colorScheme]
 * for its background and the `onSecondaryContainer` color for its label text.
 *
 * @param label The text to be displayed on the button.
 * @param icon The icon for the button.
 * @param onClick A lambda which will be invoked when the button is clicked.
 * @param modifier A [Modifier] for this composable, allowing for adjustments to its appearance
 * or behavior. This can be used to apply padding, layout, and other Modifiers.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenFilledTonalButtonWithIcon(
    label: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier,
        enabled = isEnabled,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledTonalButtonWithIcon_preview() {
    BitwardenTheme {
        BitwardenFilledTonalButtonWithIcon(
            label = "Sample Text",
            icon = rememberVectorPainter(id = R.drawable.ic_tooltip),
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
