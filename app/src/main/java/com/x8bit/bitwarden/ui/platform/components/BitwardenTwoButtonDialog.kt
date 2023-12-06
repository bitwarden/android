package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Represents a Bitwarden-styled dialog with two buttons.
 *
 * @param title the optional title to show.
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
    title: String?,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = dismissButtonText,
                onClick = onDismissClick,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = confirmButtonText,
                onClick = onConfirmClick,
            )
        },
        title = title?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
