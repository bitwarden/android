package com.bitwarden.authenticator.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * A filled tonal button for the Bitwarden UI with a customized appearance.
 *
 * This button uses the `secondaryContainer` color from the current [MaterialTheme.colorScheme]
 * for its background and the `onSecondaryContainer` color for its label text.
 *
 * @param label The text to be displayed on the button.
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
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        enabled = isEnabled,
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledTonalButton_preview() {
    AuthenticatorTheme {
        BitwardenFilledTonalButton(
            label = "Sample Text",
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
