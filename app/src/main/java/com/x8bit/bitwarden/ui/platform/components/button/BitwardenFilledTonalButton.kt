package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenFilledTonalButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A filled tonal button for the Bitwarden UI with a customized appearance.
 *
 * @param label The text to be displayed on the button.
 * @param icon The icon for the button.
 * @param onClick A lambda which will be invoked when the button is clicked.
 * @param isEnabled Whether or not the button is enabled.
 * @param modifier A [Modifier] for this composable, allowing for adjustments to its appearance
 * or behavior. This can be used to apply padding, layout, and other Modifiers.
 */
@Composable
fun BitwardenFilledTonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
) {
    Button(
        modifier = modifier.semantics(mergeDescendants = true) { },
        onClick = onClick,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        enabled = isEnabled,
        colors = bitwardenFilledTonalButtonColors(),
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledTonalButton_preview() {
    Column {
        BitwardenFilledTonalButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = true,
        )
        BitwardenFilledTonalButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = true,
        )
        BitwardenFilledTonalButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = false,
        )
        BitwardenFilledTonalButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = false,
        )
    }
}
