package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
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
 * @param supportingText An optional supporting text that will appear below the text field and
 * actions.
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
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actionsTestTag The test tag to use for the row of actions, or null if there is none.
 * @param textFieldTestTag The test tag to be used on the text field.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param cardStyle Indicates the type of card style to be applied.
 */
@Composable
fun BitwardenTextFieldWithActions(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    textStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    shouldAddCustomLineBreaks: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIconContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actionsTestTag: String? = null,
    textFieldTestTag: String? = null,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    cardStyle: CardStyle? = null,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardBackground(cardStyle = cardStyle)
            .cardPadding(cardStyle = cardStyle, vertical = 6.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BitwardenTextField(
                modifier = Modifier.weight(weight = 1f),
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
                modifier = Modifier
                    .padding(paddingValues = actionsPadding)
                    .run { actionsTestTag?.let { testTag(it) } ?: this },
                actions = actions,
            )
        }
        supportingText?.let {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 12.dp))
            Text(
                text = it,
                style = BitwardenTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun BitwardenTextFieldWithActions_preview() {
    BitwardenTheme {
        BitwardenTextFieldWithActions(
            label = "Username",
            value = "user@example.com",
            onValueChange = {},
            supportingText = "I'm here for support",
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
            cardStyle = CardStyle.Full,
        )
    }
}
