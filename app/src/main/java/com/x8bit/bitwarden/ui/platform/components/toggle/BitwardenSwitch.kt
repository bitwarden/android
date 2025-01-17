package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.toggle.color.bitwardenSwitchColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param supportingText An optional supporting text to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param cardStyle Indicates the type of card style to be applied.
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
    supportingText: String? = null,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    cardStyle: CardStyle? = null,
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
        cardStyle = cardStyle,
        actions = actions,
        supportingTextContent = supportingText?.let {
            {
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
 * @param supportingText An optional supporting text to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param cardStyle Indicates the type of card style to be applied.
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
    supportingText: String? = null,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    cardStyle: CardStyle? = null,
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
        cardStyle = cardStyle,
        actions = actions,
        supportingTextContent = supportingText?.let {
            {
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
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param supportingTextContent A lambda containing content directly below the label.
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
    cardStyle: CardStyle? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingTextContent: (@Composable ColumnScope.() -> Unit)?,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label.toAnnotatedString(),
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        readOnly = readOnly,
        enabled = enabled,
        cardStyle = cardStyle,
        actions = actions,
        supportingTextContent = supportingTextContent,
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
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param supportingTextContent A lambda containing content directly below the label.
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
    cardStyle: CardStyle? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingTextContent: @Composable (ColumnScope.() -> Unit)?,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardBackground(cardStyle = cardStyle)
            .run {
                onCheckedChange
                    ?.let {
                        this.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                color = BitwardenTheme.colorScheme.background.pressed,
                            ),
                            onClick = { it(!isChecked) },
                            enabled = !readOnly && enabled,
                        )
                    }
                    ?: this
            }
            .cardPadding(cardStyle = cardStyle)
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(isChecked)
                contentDescription?.let { this.contentDescription = it }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.weight(weight = 1f),
                verticalAlignment = Alignment.CenterVertically,
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

                actions?.invoke(this)
            }
            Spacer(modifier = Modifier.width(width = 16.dp))
            Switch(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag(tag = "SwitchToggle"),
                enabled = enabled,
                checked = isChecked,
                onCheckedChange = null,
                colors = bitwardenSwitchColors(),
            )
        }
        supportingTextContent?.let {
            Spacer(modifier = Modifier.height(height = 12.dp))
            BitwardenHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = it,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSwitch_preview() {
    Column {
        BitwardenSwitch(
            label = "Label",
            supportingText = "description",
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
            supportingText = "description",
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
