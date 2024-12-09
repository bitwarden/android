package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.tabNavigation
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.field.toolbar.BitwardenCutCopyTextToolbar
import com.x8bit.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import com.x8bit.bitwarden.ui.platform.components.model.TextToolbarType
import com.x8bit.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
 * @param keyboardActions the callbacks of keyboard actions.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 */
@Suppress("LongMethod")
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    val textToolbar = when (textToolbarType) {
        TextToolbarType.DEFAULT -> BitwardenCutCopyTextToolbar(
            value = textFieldValue,
            onValueChange = onValueChange,
            defaultTextToolbar = LocalTextToolbar.current,
            clipboardManager = LocalClipboardManager.current.nativeClipboard,
        )

        TextToolbarType.NONE -> BitwardenEmptyTextToolbar
    }
    var lastTextValue by remember(value) { mutableStateOf(value = value) }
    CompositionLocalProvider(value = LocalTextToolbar provides textToolbar) {
        OutlinedTextField(
            modifier = modifier
                .tabNavigation()
                .focusRequester(focusRequester),
            colors = bitwardenTextFieldColors(),
            textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
            label = { Text(text = label) },
            value = textFieldValue,
            onValueChange = {
                textFieldValueState = it
                val stringChangedSinceLastInvocation = lastTextValue != it.text
                lastTextValue = it.text
                if (stringChangedSinceLastInvocation) {
                    onValueChange(it.text)
                }
            },
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
            keyboardActions = keyboardActions,
            supportingText = hint?.let {
                {
                    Text(
                        text = hint,
                        style = BitwardenTheme.typography.bodySmall,
                    )
                }
            },
            trailingIcon = {
                BitwardenStandardIconButton(
                    modifier = Modifier.semantics { showPasswordTestTag?.let { testTag = it } },
                    vectorIconRes = if (showPassword) {
                        R.drawable.ic_eye_slash
                    } else {
                        R.drawable.ic_eye
                    },
                    contentDescription = stringResource(
                        id = if (showPassword) R.string.hide else R.string.show,
                    ),
                    onClick = { showPasswordChange.invoke(!showPassword) },
                )
            },
        )
    }
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
 * @param keyboardActions the callbacks of keyboard actions.
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
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
        keyboardActions = keyboardActions,
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
