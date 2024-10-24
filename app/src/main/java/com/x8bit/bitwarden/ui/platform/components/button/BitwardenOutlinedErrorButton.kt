package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenOutlinedButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled [OutlinedButton] for error states.
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenOutlinedErrorButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
) {
    BitwardenOutlinedButton(
        label = label,
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        isEnabled = isEnabled,
        colors = bitwardenOutlinedButtonColors(
            contentColor = BitwardenTheme.colorScheme.status.error,
            outlineColor = BitwardenTheme.colorScheme.status.error,
            outlineColorDisabled = BitwardenTheme.colorScheme.status.error.copy(alpha = 0.12f),
        ),
    )
}

@Preview
@Composable
private fun BBitwardenOutlinedErrorButton_preview() {
    Column {
        BitwardenOutlinedErrorButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = true,
        )
        BitwardenOutlinedErrorButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = true,
        )
        BitwardenOutlinedErrorButton(
            label = "Label",
            onClick = {},
            icon = null,
            isEnabled = false,
        )
        BitwardenOutlinedErrorButton(
            label = "Label",
            onClick = {},
            icon = rememberVectorPainter(id = R.drawable.ic_question_circle),
            isEnabled = false,
        )
    }
}
