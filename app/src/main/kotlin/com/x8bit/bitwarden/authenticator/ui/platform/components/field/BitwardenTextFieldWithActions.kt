package com.x8bit.bitwarden.authenticator.ui.platform.components.field

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * Represents a Bitwarden-styled text field accompanied by a series of actions.
 * This component allows for a more versatile design by accepting
 * icons or actions next to the text field.
 *
 * @param label Label for the text field.
 * @param value Current text in the text field.
 * @param onValueChange Callback that is triggered when the text content changes.
 * @param modifier [Modifier] applied to this layout composable.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param trailingIconContent the content for the trailing icon in the text field.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * next to the text field. This lambda extends [RowScope],
 * providing flexibility in the layout definition.
 * @param textFieldTestTag The test tag to be used on the text field.
 */
@Composable
fun BitwardenTextFieldWithActions(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    shouldAddCustomLineBreaks: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIconContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionsTestTag: String? = null,
    textFieldTestTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenTextField(
            modifier = Modifier
                .semantics { textFieldTestTag?.let { testTag = it } }
                .weight(1f),
            label = label,
            value = value,
            readOnly = readOnly,
            singleLine = singleLine,
            onValueChange = onValueChange,
            keyboardType = keyboardType,
            trailingIconContent = trailingIconContent,
            textStyle = textStyle,
            shouldAddCustomLineBreaks = shouldAddCustomLineBreaks,
            visualTransformation = visualTransformation,
        )
        BitwardenRowOfActions(
            modifier = Modifier.run { actionsTestTag?.let { semantics { testTag = it } } ?: this },
            actions = actions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenTextFieldWithActions_preview() {
    AuthenticatorTheme {
        BitwardenTextFieldWithActions(
            label = "Username",
            value = "user@example.com",
            onValueChange = {},
            actions = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tooltip),
                    contentDescription = "Action 1",
                )
            },
        )
    }
}
