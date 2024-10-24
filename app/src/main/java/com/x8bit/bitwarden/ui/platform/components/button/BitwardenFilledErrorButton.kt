package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenFilledErrorButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter

/**
 * Represents a Bitwarden-styled filled [Button] for error scenarios.
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenFilledErrorButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
) {
    BitwardenFilledButton(
        label = label,
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        isEnabled = isEnabled,
        colors = bitwardenFilledErrorButtonColors(),
    )
}

@Preview
@Composable
private fun BitwardenErrorButton_preview() {
    Column {
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = true,
        )
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = true,
        )
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = false,
        )
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = false,
        )
    }
}
