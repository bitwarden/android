package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Represents a Bitwarden-styled filled [OutlinedButton] for error states.
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenOutlinedErrorButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    OutlinedButton(
        modifier = modifier.semantics(mergeDescendants = true) { },
        onClick = onClick,
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(
                alpha = if (isEnabled) 1f else 0.12f,
            ),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenOutlinedErrorButton_preview_isEnabled() {
    BitwardenOutlinedErrorButton(
        label = "Label",
        onClick = {},
        isEnabled = true,
    )
}

@Preview
@Composable
private fun BBitwardenOutlinedErrorButton_preview_isNotEnabled() {
    BitwardenOutlinedErrorButton(
        label = "Label",
        onClick = {},
        isEnabled = false,
    )
}
