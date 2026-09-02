package com.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.core.data.util.toFormattedDateStyle
import com.bitwarden.ui.platform.components.animation.AnimateNullableContentVisibility
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.button.model.BitwardenHelpButtonData
import com.bitwarden.ui.platform.components.dialog.BitwardenDatePickerDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import java.time.Clock
import java.time.LocalDate
import java.time.format.FormatStyle

/**
 * A button that displays a selected date and opens a date picker dialog when clicked.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param currentDate The currently selected [LocalDate] value.
 * @param onDateSelect A lambda invoked with the newly selected [LocalDate] when confirmed.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param isEnabled Whether the button is enabled.
 * @param supportingContent An optional supporting content that will appear below the button.
 * @param helpData An optional [BitwardenHelpButtonData], representing the help button.
 * @param insets Inner padding to be applied within the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param allowPastDates Indicates that the date picker allows past dates.
 * @param allowFutureDates Indicates that the date picker allows future dates.
 * @param allowToday Indicates that this date picker allows this date to be selected.
 * @param clock The system clock.
 */
@Composable
fun BitwardenDatePickerButton(
    label: String,
    currentDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingContent: @Composable (ColumnScope.() -> Unit)? = null,
    helpData: BitwardenHelpButtonData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    allowPastDates: Boolean = true,
    allowFutureDates: Boolean = true,
    allowToday: Boolean = true,
    clock: Clock = LocalClock.current,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    val openCalendarString = stringResource(id = BitwardenString.open_calendar)
    BitwardenTextSelectionButton(
        label = label,
        selectedOption = currentDate?.toFormattedDateStyle(
            dateStyle = FormatStyle.LONG,
            clock = clock,
        ),
        onClick = { shouldShowDialog = true },
        cardStyle = cardStyle,
        enabled = isEnabled,
        showChevron = false,
        supportingContent = supportingContent,
        helpData = helpData,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actions = {
            AnimateNullableContentVisibility(targetState = currentDate) {
                BitwardenStandardIconButton(
                    vectorIconRes = BitwardenDrawable.ic_clear,
                    contentDescription = stringResource(id = BitwardenString.clear),
                    onClick = { onDateSelect(null) },
                    isEnabled = isEnabled,
                )
            }
            Icon(
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        },
        modifier = modifier.semantics(mergeDescendants = true) {
            this.onClick(label = openCalendarString, action = null)
        },
    )
    if (shouldShowDialog) {
        BitwardenDatePickerDialog(
            initialDate = currentDate,
            onDateSelect = { date ->
                onDateSelect(date)
                shouldShowDialog = false
            },
            onDismissRequest = { shouldShowDialog = false },
            allowPastDates = allowPastDates,
            allowFutureDates = allowFutureDates,
            allowToday = allowToday,
            clock = clock,
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun BitwardenDatePickerButton_preview() {
    BitwardenTheme {
        BitwardenDatePickerButton(
            label = "Date of birth",
            currentDate = LocalDate.of(2026, 6, 15),
            onDateSelect = {},
            cardStyle = CardStyle.Full,
        )
    }
}
