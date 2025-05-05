package com.bitwarden.authenticator.ui.platform.components.button

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
 * Represents a Bitwarden-styled filled [OutlinedButton].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .semantics(mergeDescendants = true) { },
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = ButtonDefaults.outlinedButtonColors(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenOutlinedButton_preview_isEnabled() {
    BitwardenOutlinedButton(
        label = "Label",
        onClick = {},
        isEnabled = true,
    )
}

@Preview
@Composable
private fun BitwardenOutlinedButton_preview_isNotEnabled() {
    BitwardenOutlinedButton(
        label = "Label",
        onClick = {},
        isEnabled = false,
    )
}
