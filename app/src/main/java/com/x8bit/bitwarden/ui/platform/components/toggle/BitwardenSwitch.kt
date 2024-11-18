package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.toggle.color.bitwardenSwitchColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param description An optional description label to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label.toAnnotatedString(),
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        readOnly = readOnly,
        enabled = enabled,
        actions = actions,
        subContent = {
            description?.let {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = if (enabled) {
                        BitwardenTheme.colorScheme.text.secondary
                    } else {
                        BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                    },
                )
            }
        },
    )
}

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param description An optional description label to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenSwitch(
    label: AnnotatedString,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        readOnly = readOnly,
        enabled = enabled,
        actions = actions,
        subContent = {
            description?.let {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = if (enabled) {
                        BitwardenTheme.colorScheme.text.secondary
                    } else {
                        BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                    },
                )
            }
        },
    )
}

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param subContent A lambda containing content directly below the label.
 */
@Composable
fun BitwardenSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    subContent: @Composable () -> Unit,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label.toAnnotatedString(),
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        readOnly = readOnly,
        enabled = enabled,
        actions = actions,
        subContent = subContent,
    )
}

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param subContent A lambda containing content directly below the label.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenSwitch(
    label: AnnotatedString,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    subContent: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = { onCheckedChange?.invoke(!isChecked) },
                enabled = !readOnly && enabled,
            )
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(isChecked)
                contentDescription?.let { this.contentDescription = it }
            }
            .then(modifier),
    ) {
        Row(
            modifier = Modifier.weight(weight = 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = if (enabled) {
                        BitwardenTheme.colorScheme.text.primary
                    } else {
                        BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                    },
                    modifier = Modifier.testTag(tag = "SwitchText"),
                )
                subContent()
            }

            actions
                ?.invoke(this)
                ?: Spacer(modifier = Modifier.width(width = 16.dp))
        }

        Switch(
            modifier = Modifier
                .height(height = 56.dp)
                .testTag(tag = "SwitchToggle"),
            enabled = enabled,
            checked = isChecked,
            onCheckedChange = null,
            colors = bitwardenSwitchColors(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSwitch_preview() {
    Column {
        BitwardenSwitch(
            label = "Label",
            description = "description",
            isChecked = true,
            onCheckedChange = {},
        )
        BitwardenSwitch(
            label = "Label",
            isChecked = false,
            onCheckedChange = {},
        )
        BitwardenSwitch(
            label = "Label",
            description = "description",
            isChecked = true,
            onCheckedChange = {},
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_question_circle,
                    contentDescription = "content description",
                    onClick = {},
                )
            },
        )
        BitwardenSwitch(
            label = "Label",
            isChecked = false,
            onCheckedChange = {},
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_question_circle,
                    contentDescription = "content description",
                    onClick = {},
                )
            },
        )
    }
}
