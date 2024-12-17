package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTonalIconButton
import com.x8bit.bitwarden.ui.platform.components.model.TextToolbarType
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller
 * and provides additional actions.
 *
 * See overloaded [BitwardenPasswordFieldWithActions] for managed show/hide state.
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
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenPasswordFieldWithActions(
    label: String,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    showPasswordTestTag: String? = null,
    passwordFieldTestTag: String? = null,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        BitwardenPasswordField(
            label = label,
            value = value,
            showPasswordChange = showPasswordChange,
            showPassword = showPassword,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            modifier = Modifier
                .semantics { passwordFieldTestTag?.let { testTag = it } }
                .weight(1f)
                .padding(end = 8.dp),
            showPasswordTestTag = showPasswordTestTag,
            textToolbarType = textToolbarType,
        )
        actions()
    }
}

/**
 * Represents a Bitwarden-styled password field that manages the state of a show/hide indicator
 * internally.
 *
 * See overloaded [BitwardenPasswordFieldWithActions] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param initialShowPassword The initial state of the show/hide password control. A value of
 * `false` (the default) indicates that that password should begin in the hidden state.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenPasswordFieldWithActions(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    initialShowPassword: Boolean = false,
    showPasswordTestTag: String? = null,
    passwordFieldTestTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var shouldShowPassword by remember { mutableStateOf(initialShowPassword) }
    BitwardenPasswordFieldWithActions(
        label = label,
        value = value,
        showPassword = shouldShowPassword,
        showPasswordChange = { shouldShowPassword = !shouldShowPassword },
        onValueChange = onValueChange,
        modifier = modifier,
        readOnly = readOnly,
        singleLine = singleLine,
        showPasswordTestTag = showPasswordTestTag,
        passwordFieldTestTag = passwordFieldTestTag,
        actions = actions,
    )
}

@Preview(showBackground = true)
@Composable
private fun BitwardenPasswordFieldWithActions_preview() {
    BitwardenTheme {
        BitwardenPasswordFieldWithActions(
            label = "Password",
            value = "samplePassword",
            onValueChange = {},
            actions = {
                BitwardenTonalIconButton(
                    vectorIconRes = R.drawable.ic_check_mark,
                    contentDescription = "",
                    onClick = {},
                )
            },
        )
    }
}
