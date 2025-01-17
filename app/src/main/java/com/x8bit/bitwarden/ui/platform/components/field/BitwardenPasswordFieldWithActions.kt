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
import androidx.compose.material3.Text
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
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.TextToolbarType
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
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
 * @param supportingText An optional supporting text that will appear below the text field and
 * actions.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
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
    supportingText: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    showPasswordTestTag: String? = null,
    passwordFieldTestTag: String? = null,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    cardStyle: CardStyle? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
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
            BitwardenPasswordField(
                label = label,
                value = value,
                showPasswordChange = showPasswordChange,
                showPassword = showPassword,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = singleLine,
                showPasswordTestTag = showPasswordTestTag,
                textToolbarType = textToolbarType,
                modifier = Modifier
                    .semantics { passwordFieldTestTag?.let { testTag = it } }
                    .weight(weight = 1f),
            )
            BitwardenRowOfActions(
                modifier = Modifier.padding(paddingValues = actionsPadding),
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
 * @param cardStyle Indicates the type of card style to be applied.
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param actionsPadding Padding to be applied to the [actions] block.
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
    cardStyle: CardStyle? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
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
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

@Preview
@Composable
private fun BitwardenPasswordFieldWithActions_preview() {
    BitwardenTheme {
        BitwardenPasswordFieldWithActions(
            label = "Password",
            value = "samplePassword",
            onValueChange = {},
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_check_mark,
                    contentDescription = "",
                    onClick = {},
                )
            },
            cardStyle = CardStyle.Full,
        )
    }
}
