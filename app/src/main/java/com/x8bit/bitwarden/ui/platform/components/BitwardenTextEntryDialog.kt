package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden-styled dialog that is used to enter text.
 *
 * @param title The optional title to show.
 * @param textFieldLabel Label for the text field.
 * @param onConfirmClick Called when the confirm button is clicked.
 * @param onDismissRequest Called when the user attempts to dismiss the dialog.
 */
@Composable
fun BitwardenTextEntryDialog(
    title: String?,
    textFieldLabel: String,
    onConfirmClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = R.string.cancel),
                onClick = onDismissRequest,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = R.string.ok),
                onClick = { onConfirmClick(text) },
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
            BitwardenTextField(
                label = textFieldLabel,
                value = text,
                onValueChange = { text = it },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
