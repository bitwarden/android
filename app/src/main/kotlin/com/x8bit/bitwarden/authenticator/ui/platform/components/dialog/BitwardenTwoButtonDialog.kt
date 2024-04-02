package com.x8bit.bitwarden.authenticator.ui.platform.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton

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
 * @param confirmTextColor The color of the confirm text.
 * @param dismissTextColor The color of the dismiss text.
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
    confirmTextColor: Color? = null,
    dismissTextColor: Color? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = dismissButtonText,
                labelTextColor = dismissTextColor,
                onClick = onDismissClick,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = confirmButtonText,
                labelTextColor = confirmTextColor,
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
