package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.TextToolbarType
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
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
 * @param textStyle The [TextStyle], or null if default.
 * @param shouldAddCustomLineBreaks If `true`, line breaks will be inserted to allow for filling
 * an entire line before breaking. `false` by default.
 * @param visualTransformation Transforms the visual representation of the input [value].
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param keyboardType the preferred type of keyboard input.
 * @param trailingIconContent the content for the trailing icon in the text field.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * next to the text field. This lambda extends [RowScope],
 * providing flexibility in the layout definition.
 * @param actionsTestTag The test tag to use for the row of actions, or null if there is none.
 * @param textFieldTestTag The test tag to be used on the text field.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 */
@Composable
fun BitwardenTextFieldWithActions(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    shouldAddCustomLineBreaks: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIconContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionsTestTag: String? = null,
    textFieldTestTag: String? = null,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenTextField(
            modifier = Modifier.weight(1f),
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
            textToolbarType = textToolbarType,
            textFieldTestTag = textFieldTestTag,
        )
        BitwardenRowOfActions(
            modifier = Modifier.run { actionsTestTag?.let { testTag(it) } ?: this },
            actions = actions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenTextFieldWithActions_preview() {
    BitwardenTheme {
        BitwardenTextFieldWithActions(
            label = "Username",
            value = "user@example.com",
            onValueChange = {},
            actions = {
                Icon(
                    painter = rememberVectorPainter(id = R.drawable.ic_question_circle),
                    contentDescription = "Action 1",
                )
                Icon(
                    painter = rememberVectorPainter(id = R.drawable.ic_generate),
                    contentDescription = "Action 2",
                )
            },
        )
    }
}
