package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller.
 *
 * See overloaded [BitwardenPasswordField] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param showPassword Whether or not password should be shown.
 * @param showPasswordChange Lambda that is called when user request show/hide be toggled.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param hint optional hint text that will appear below the text input.
 */
@Composable
fun BitwardenPasswordField(
    label: String,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            label = { Text(text = label) },
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = { showPasswordChange.invoke(!showPassword) },
                ) {
                    if (showPassword) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_visibility_off),
                            contentDescription = stringResource(id = R.string.hide),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_visibility),
                            contentDescription = stringResource(id = R.string.show),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )

        hint?.let {
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

/**
 * Represents a Bitwarden-styled password field that manages the state of a show/hide indicator
 * internally.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param hint optional hint text that will appear below the text input.
 * @param initialShowPassword The initial state of the show/hide password control. A value of
 * `false` (the default) indicates that that password should begin in the hidden state.
 */
@Composable
fun BitwardenPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    initialShowPassword: Boolean = false,
) {
    var showPassword by rememberSaveable { mutableStateOf(initialShowPassword) }
    BitwardenPasswordField(
        modifier = modifier,
        label = label,
        value = value,
        showPassword = showPassword,
        showPasswordChange = { showPassword = !showPassword },
        onValueChange = onValueChange,
        hint = hint,
    )
}

@Preview
@Composable
private fun BitwardenPasswordField_preview_withInput_hidePassword() {
    BitwardenPasswordField(
        label = "Label",
        value = "Password",
        onValueChange = {},
        initialShowPassword = false,
        hint = "Hint",
    )
}

@Preview
@Composable
private fun BitwardenPasswordField_preview_withInput_showPassword() {
    BitwardenPasswordField(
        label = "Label",
        value = "Password",
        onValueChange = {},
        initialShowPassword = true,
        hint = "Hint",
    )
}

@Preview
@Composable
private fun BitwardenPasswordField_preview_withoutInput_hidePassword() {
    BitwardenPasswordField(
        label = "Label",
        value = "",
        onValueChange = {},
        initialShowPassword = false,
        hint = "Hint",
    )
}

@Preview
@Composable
private fun BitwardenPasswordField_preview_withoutInput_showPassword() {
    BitwardenPasswordField(
        label = "Label",
        value = "",
        onValueChange = {},
        initialShowPassword = true,
        hint = "Hint",
    )
}
