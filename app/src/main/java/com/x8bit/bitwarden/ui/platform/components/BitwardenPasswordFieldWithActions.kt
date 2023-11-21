package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller
 * and provides additional actions.
 *
 * See overloaded [BitwardenPasswordField] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
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
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        BitwardenPasswordField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        actions()
    }
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
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_check_mark),
                        contentDescription = "",
                    ),
                    onClick = {},
                )
            },
        )
    }
}
