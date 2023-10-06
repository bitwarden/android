package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Component that allows the user to input text. This composable will manage the state of
 * the user's input.
 * @param label label for the text field.
 * @param value current next on the text field.
 * @param modifier modifier for the composable.
 * @param onValueChange callback that is triggered when the input of the text field changes.
 */
@Composable
fun BitwardenTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        label = { Text(text = label) },
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
    )
}

@Preview
@Composable
private fun BitwardenTextField_preview_withInput() {
    BitwardenTextField(
        label = "Label",
        value = "Input",
        onValueChange = {},
    )
}

@Preview
@Composable
private fun BitwardenTextField_preview_withoutInput() {
    BitwardenTextField(
        label = "Label",
        value = "",
        onValueChange = {},
    )
}
