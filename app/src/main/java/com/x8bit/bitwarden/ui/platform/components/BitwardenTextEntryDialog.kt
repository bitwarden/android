package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden-styled dialog that is used to enter text.
 *
 * @param title The optional title to show.
 * @param textFieldLabel Label for the text field.
 * @param onConfirmClick Called when the confirm button is clicked.
 * @param onDismissRequest Called when the user attempts to dismiss the dialog.
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * @param initialText The text that will be visible at the start of text entry.
 */
@Composable
fun BitwardenTextEntryDialog(
    title: String?,
    textFieldLabel: String,
    onConfirmClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
    autoFocus: Boolean = false,
    initialText: String? = null,
) {
    var text by remember { mutableStateOf(initialText.orEmpty()) }
    val focusRequester = remember { FocusRequester() }
    var shouldRequestFocus by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onGloballyPositioned {
                        shouldRequestFocus = true
                    },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    if (autoFocus && shouldRequestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
