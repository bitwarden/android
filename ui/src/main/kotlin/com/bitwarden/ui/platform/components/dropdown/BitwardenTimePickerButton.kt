package com.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTimePickerDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString

private const val MINUTES_PER_HOUR: Int = 60

/**
 * A button that displays a selected time duration and opens a time picker dialog when clicked.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param totalMinutes The currently selected time value in minutes.
 * @param onTimeSelect A lambda that is invoked when a time is selected from the menu.
 * @param is24Hour Whether or not the time should be displayed in 24-hour format.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param isEnabled Whether or not the button is enabled.
 * @param supportingContent An optional supporting content that will appear below the button.
 * @param tooltip A nullable [TooltipData], representing the tooltip icon.
 * @param insets Inner padding to be applied within the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenTimePickerButton(
    label: String,
    totalMinutes: Int,
    onTimeSelect: (minutes: Int) -> Unit,
    is24Hour: Boolean,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
) {
    BitwardenTimePickerButton(
        label = label,
        hours = totalMinutes / MINUTES_PER_HOUR,
        minutes = totalMinutes.mod(MINUTES_PER_HOUR),
        onTimeSelect = { hour, minute -> onTimeSelect((hour * MINUTES_PER_HOUR) + minute) },
        cardStyle = cardStyle,
        is24Hour = is24Hour,
        modifier = modifier,
        isEnabled = isEnabled,
        supportingContent = supportingContent,
        tooltip = tooltip,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

/**
 * A button that displays a selected time duration and opens a time picker dialog when clicked.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param hours The currently selected time value in hours.
 * @param minutes The currently selected time value in minutes.
 * @param onTimeSelect A lambda that is invoked when a time is selected from the menu.
 * @param is24Hour Whether or not the time should be displayed in 24-hour format.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param isEnabled Whether or not the button is enabled.
 * @param supportingContent An optional supporting content that will appear below the button.
 * @param tooltip A nullable [TooltipData], representing the tooltip icon.
 * @param insets Inner padding to be applied within the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenTimePickerButton(
    label: String,
    hours: Int,
    minutes: Int,
    onTimeSelect: (hour: Int, minute: Int) -> Unit,
    is24Hour: Boolean,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    BitwardenTextSelectionButton(
        label = label,
        selectedOption = if (hours != 0 && minutes != 0) {
            // Since both hours and minutes are non-zero, we display both of them.
            stringResource(
                id = BitwardenString.hours_minutes_format,
                formatArgs = arrayOf(
                    pluralStringResource(
                        id = BitwardenPlurals.hours_format,
                        count = hours,
                        formatArgs = arrayOf(hours),
                    ),
                    pluralStringResource(
                        id = BitwardenPlurals.minutes_format,
                        count = minutes,
                        formatArgs = arrayOf(minutes),
                    ),
                ),
            )
        } else if (hours != 0) {
            // Since only hours are non-zero, we only display hours.
            pluralStringResource(
                id = BitwardenPlurals.hours_format,
                count = hours,
                formatArgs = arrayOf(hours),
            )
        } else {
            // We display this if there are only minutes or if both hours and minutes are 0.
            pluralStringResource(
                id = BitwardenPlurals.minutes_format,
                count = minutes,
                formatArgs = arrayOf(minutes),
            )
        },
        onClick = { shouldShowDialog = true },
        cardStyle = cardStyle,
        enabled = isEnabled,
        showChevron = false,
        supportingContent = supportingContent,
        tooltip = tooltip,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actionsPadding = actionsPadding,
        actions = actions,
        modifier = modifier,
    )
    if (shouldShowDialog) {
        BitwardenTimePickerDialog(
            initialHour = hours,
            initialMinute = minutes,
            onTimeSelect = { hour, minute ->
                onTimeSelect(hour, minute)
                shouldShowDialog = false
            },
            onDismissRequest = { shouldShowDialog = false },
            is24Hour = is24Hour,
        )
    }
}
