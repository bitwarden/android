package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview

/**
 * Component that allows the user to input text. This composable will manage the state of
 * the user's input.
 * @param label label for the text field.
 * @param value current next on the text field.
 * @param modifier modifier for the composable.
 * @param onValueChange callback that is triggered when the input of the text field changes.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the [value] is empty.
 * @param hint optional hint text that will appear below the text input.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param enabled Whether or not the text field is enabled.
 * @param keyboardType the preferred type of keyboard input.
 */
@Composable
fun BitwardenTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        label = { Text(text = label) },
        value = value,
        placeholder = placeholder?.let {
            { Text(text = it) }
        },
        supportingText = hint?.let {
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        onValueChange = onValueChange,
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
    )
}

@Preview
@Composable
private fun BitwardenTextField_preview_withInput() {
    BitwardenTextField(
        label = "Label",
        value = "Input",
        onValueChange = {},
        hint = "Hint",
    )
}

@Preview
@Composable
private fun BitwardenTextField_preview_withoutInput() {
    BitwardenTextField(
        label = "Label",
        value = "",
        onValueChange = {},
        hint = "Hint",
    )
}
