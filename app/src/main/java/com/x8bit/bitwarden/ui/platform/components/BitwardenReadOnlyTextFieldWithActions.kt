package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden-styled text field accompanied by a series of actions.
 * This component allows for a more versatile design by accepting
 * icons or actions next to the text field. This composable is read-only and because it uses
 * the BitwardenTextField we clear the semantics here to prevent talk back from clarifying the
 * component is "editable" or "disabled".
 *
 * @param label Label for the text field.
 * @param value Current text in the text field.
 * @param modifier [Modifier] applied to this layout composable.
 * @param readOnly If `true`, user input is disabled for the text field, making it read-only.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * next to the text field. This lambda extends [RowScope],
 * providing flexibility in the layout definition.
 */
@Composable
fun BitwardenReadOnlyTextFieldWithActions(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenTextField(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = "$label, $value"
                    text = AnnotatedString(label)
                },
            readOnly = readOnly,
            label = label,
            value = value,
            onValueChange = {},
        )
        BitwardenRowOfActions(actions)
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenReadOnlyTextFieldWithActions_preview() {
    BitwardenReadOnlyTextFieldWithActions(
        label = "Username",
        value = "john.doe",
        readOnly = true,
        actions = {
            Icon(
                painter = painterResource(id = R.drawable.ic_tooltip),
                contentDescription = "",
                modifier = Modifier.size(24.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_tooltip),
                contentDescription = "",
                modifier = Modifier.size(24.dp),
            )
        },
    )
}
