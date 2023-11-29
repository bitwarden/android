package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * next to the text field. This lambda extends [RowScope],
 * providing flexibility in the layout definition.
 */
@Composable
fun BitwardenTextFieldWithActions(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenTextField(
            modifier = Modifier
                .weight(1f),
            label = label,
            value = value,
            readOnly = readOnly,
            singleLine = singleLine,
            onValueChange = onValueChange,
            keyboardType = keyboardType,
        )
        BitwardenRowOfActions(actions)
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenTextFieldWithActions_preview() {
    BitwardenTheme {
        BitwardenReadOnlyTextFieldWithActions(
            label = "Username",
            value = "user@example.com",
            actions = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tooltip),
                    contentDescription = "Action 1",
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_generator),
                    contentDescription = "Action 2",
                )
            },
        )
    }
}
