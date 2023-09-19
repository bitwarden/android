package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    TextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        label = { Text(label) },
        value = value,
        onValueChange = onValueChange,
    )
}
