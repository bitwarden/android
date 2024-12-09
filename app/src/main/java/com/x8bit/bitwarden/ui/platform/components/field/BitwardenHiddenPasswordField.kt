package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled password field that is completely hidden and non-interactable.
 *
 * @param label Label for the text field.
 * @param value Current text on the text field.
 * @param modifier Modifier for the composable.
 */
@Composable
fun BitwardenHiddenPasswordField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(value = LocalTextToolbar provides BitwardenEmptyTextToolbar) {
        OutlinedTextField(
            modifier = modifier,
            textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
            label = { Text(text = label) },
            value = value,
            onValueChange = { },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = false,
            readOnly = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = bitwardenTextFieldColors(),
        )
    }
}

@Preview
@Composable
private fun BitwardenHiddenPasswordField_preview() {
    BitwardenTheme {
        BitwardenHiddenPasswordField(
            label = "Label",
            value = "Password",
        )
    }
}
