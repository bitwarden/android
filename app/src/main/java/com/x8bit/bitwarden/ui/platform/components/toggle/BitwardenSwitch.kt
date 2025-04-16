package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.toggle.color.bitwardenSwitchColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param subtext The text to be displayed under the [label].
 * @param supportingText An optional supporting text to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param tooltip The data required to display a tooltip.
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
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    supportingText: String? = null,
    contentDescription: String? = null,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label.toAnnotatedString(),
        subtext = subtext,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        tooltip = tooltip,
        readOnly = readOnly,
        enabled = enabled,
        cardStyle = cardStyle,
        actions = actions,
        supportingContent = supportingText?.let {
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
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param subtext The text to be displayed under the [label].
 * @param supportingText An optional supporting text to be displayed below the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param tooltip The data required to display a tooltip.
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
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    supportingText: String? = null,
    contentDescription: String? = null,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label,
        subtext = subtext,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        tooltip = tooltip,
        readOnly = readOnly,
        enabled = enabled,
        cardStyle = cardStyle,
        actions = actions,
        supportingContent = supportingText?.let {
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
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param subtext The text to be displayed under the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param supportingContent A lambda containing content directly below the label.
 */
@Composable
fun BitwardenSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    contentDescription: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: (@Composable ColumnScope.() -> Unit)?,
) {
    BitwardenSwitch(
        modifier = modifier,
        label = label.toAnnotatedString(),
        subtext = subtext,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        contentDescription = contentDescription,
        readOnly = readOnly,
        enabled = enabled,
        cardStyle = cardStyle,
        actions = actions,
        supportingContent = supportingContent,
    )
}

/**
 * A custom switch composable
 *
 * @param label The descriptive text label to be displayed adjacent to the switch.
 * @param isChecked The current state of the switch (either checked or unchecked).
 * @param onCheckedChange A lambda that is invoked when the switch's state changes.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param subtext The text to be displayed under the [label].
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 * @param tooltip The data required to display a tooltip.
 * @param readOnly Disables the click functionality without modifying the other UI characteristics.
 * @param enabled Whether or not this switch is enabled. This is similar to setting [readOnly] but
 * comes with some additional visual changes.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in between the [label] and the toggle. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 * @param supportingContent A lambda containing content directly below the label.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun BitwardenSwitch(
    label: AnnotatedString,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    contentDescription: String? = null,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    supportingContentPadding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onCheckedChange?.let { { it(!isChecked) } },
                clickEnabled = !readOnly && enabled,
                paddingTop = 6.dp,
                paddingBottom = 0.dp,
            )
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(isChecked)
                contentDescription?.let { this.contentDescription = it }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        ) {
            Spacer(modifier = Modifier.width(width = 16.dp))
            Row(
                modifier = Modifier.weight(weight = 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(weight = 1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        tooltip?.let {
                            ToolTip(
                                tooltip = it,
                                isVisible = subtext != null,
                                size = 16.dp,
                            )
                        }
                    }
                    subtext?.let {
                        Spacer(modifier = Modifier.height(height = 2.dp))
                        Text(
                            text = it,
                            style = BitwardenTheme.typography.bodyMedium,
                            color = if (enabled) {
                                BitwardenTheme.colorScheme.text.secondary
                            } else {
                                BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(tag = "SwitchSubtext"),
                        )
                    }
                }
                tooltip?.let { ToolTip(tooltip = it, isVisible = subtext == null) }
            }
            Spacer(modifier = Modifier.width(width = 16.dp))
            Switch(
                modifier = Modifier
                    .height(height = 32.dp)
                    .testTag(tag = "SwitchToggle"),
                enabled = enabled,
                checked = isChecked,
                onCheckedChange = null,
                colors = bitwardenSwitchColors(),
            )
            actions?.let { BitwardenRowOfActions(actions = it) }
            Spacer(modifier = Modifier.width(width = if (actions == null) 16.dp else 4.dp))
        }
        supportingContent
            ?.let { content ->
                SupportingContent(
                    paddingValues = supportingContentPadding,
                    content = content,
                )
            }
            ?: Spacer(modifier = Modifier.height(height = cardStyle?.let { 6.dp } ?: 0.dp))
    }
}

@Composable
private fun ColumnScope.SupportingContent(
    paddingValues: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(modifier = Modifier.height(height = 12.dp))
    BitwardenHorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
    )
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .padding(paddingValues = paddingValues),
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun RowScope.ToolTip(
    tooltip: TooltipData,
    isVisible: Boolean,
    size: Dp = 48.dp,
) {
    if (!isVisible) return
    Spacer(modifier = Modifier.width(width = 8.dp))
    BitwardenStandardIconButton(
        vectorIconRes = R.drawable.ic_question_circle_small,
        contentDescription = tooltip.contentDescription,
        onClick = tooltip.onClick,
        contentColor = BitwardenTheme.colorScheme.icon.secondary,
        modifier = Modifier
            .size(size = size)
            .testTag(tag = "SwitchTooltip"),
    )
}

@Suppress("LongMethod")
@Preview(wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
private fun BitwardenSwitch_preview() {
    BitwardenTheme(dynamicColor = true) {
        Column {
            BitwardenSwitch(
                label = "Label",
                supportingText = "description",
                isChecked = true,
                onCheckedChange = {},
                cardStyle = CardStyle.Top(),
            )
            BitwardenSwitch(
                label = "Label",
                isChecked = false,
                onCheckedChange = {},
                cardStyle = CardStyle.Middle(),
            )
            BitwardenSwitch(
                label = "Label",
                supportingText = "description",
                isChecked = true,
                onCheckedChange = {},
                tooltip = TooltipData(
                onClick = { },
                contentDescription = "content description",
            ),
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_generate,
                        contentDescription = "content description",
                        onClick = {},
                    )
                },
            cardStyle = CardStyle.Middle(),
        )
        BitwardenSwitch(
            label = "Label",
            supportingText = "description",
            isChecked = true,
            onCheckedChange = {},
            tooltip = TooltipData(
                onClick = { },
                contentDescription = "content description",
            ),
            cardStyle = CardStyle.Middle(),
        )
            BitwardenSwitch(
                label = "Label",
                isChecked = false,
                onCheckedChange = {},
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_generate,
                        contentDescription = "content description",
                        onClick = {},
                    )
                },
                cardStyle = CardStyle.Bottom,
            )
        }
    }
}
