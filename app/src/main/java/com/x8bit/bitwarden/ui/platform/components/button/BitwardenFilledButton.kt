package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenFilledButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled [Button].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenFilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
    colors: ButtonColors = bitwardenFilledButtonColors(),
) {
    Button(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick,
        enabled = isEnabled,
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = if (icon == null) 24.dp else 16.dp,
            end = 24.dp,
        ),
        colors = colors,
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

@Preview
@Composable
private fun BitwardenFilledButton_preview() {
    Column {
        BitwardenFilledButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = true,
        )
        BitwardenFilledButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = true,
        )
        BitwardenFilledButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = false,
        )
        BitwardenFilledButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = false,
        )
    }
}
