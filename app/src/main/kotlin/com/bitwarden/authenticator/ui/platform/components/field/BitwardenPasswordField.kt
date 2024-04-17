package com.bitwarden.authenticator.ui.platform.components.field

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.util.nonLetterColorVisualTransformation

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
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param hint optional hint text that will appear below the text input.
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 */
@Composable
fun BitwardenPasswordField(
    label: String,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
) {
    val focusRequester = remember { FocusRequester() }
    OutlinedTextField(
        modifier = modifier.focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        value = value,
        onValueChange = onValueChange,
        visualTransformation = when {
            !showPassword -> PasswordVisualTransformation()
            readOnly -> nonLetterColorVisualTransformation()
            else -> VisualTransformation.None
        },
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        supportingText = hint?.let {
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { showPasswordChange.invoke(!showPassword) },
            ) {
                @DrawableRes
                val painterRes = if (showPassword) {
                    R.drawable.ic_visibility_off
                } else {
                    R.drawable.ic_visibility
                }

                @StringRes
                val contentDescriptionRes = if (showPassword) R.string.hide else R.string.show
                Icon(
                    modifier = Modifier.semantics { showPasswordTestTag?.let { testTag = it } },
                    painter = painterResource(id = painterRes),
                    contentDescription = stringResource(id = contentDescriptionRes),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param hint optional hint text that will appear below the text input.
 * @param initialShowPassword The initial state of the show/hide password control. A value of
 * `false` (the default) indicates that that password should begin in the hidden state.
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 */
@Composable
fun BitwardenPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null,
    initialShowPassword: Boolean = false,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
) {
    var showPassword by rememberSaveable { mutableStateOf(initialShowPassword) }
    BitwardenPasswordField(
        modifier = modifier,
        label = label,
        value = value,
        showPassword = showPassword,
        showPasswordChange = { showPassword = !showPassword },
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        hint = hint,
        showPasswordTestTag = showPasswordTestTag,
        autoFocus = autoFocus,
        keyboardType = keyboardType,
        imeAction = imeAction,
    )
}

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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
