package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * Represents a Bitwarden-styled dialog that is hidden or shown based on [visibilityState].
 *
 * @param title title to show.
 * @param message message to show.
 * @param confirmButtonText text to show on confirm button.
 * @param dismissButtonText text to show on dismiss button.
 * @param onConfirmClick called when the confirm button is clicked.
 * @param onDismissClick called when the dismiss button is clicked.
 * @param onDismissRequest called when the user attempts to dismiss the dialog (for example by
 * tapping outside of it).
 */
@Composable
fun BitwardenTwoButtonDialog(
    title: Text,
    message: Text,
    confirmButtonText: Text,
    dismissButtonText: Text,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = dismissButtonText(),
                onClick = onDismissClick,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = confirmButtonText(),
                onClick = onConfirmClick,
            )
        },
        title = {
            Text(
                text = title(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = message(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
