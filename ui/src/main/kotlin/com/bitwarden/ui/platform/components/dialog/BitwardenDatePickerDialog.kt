package com.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A custom composable representing a dialog that displays a date picker.
 *
 * @param initialDate The initial [LocalDate] to display.
 * @param onDateSelect The callback invoked with the selected [LocalDate] when the user confirms.
 * @param onDismissRequest The callback invoked when the dialog is dismissed.
 * @param allowPastDates Indicates that this date picker allows past dates.
 * @param allowFutureDates Indicates that this date picker allows future dates.
 * @param allowToday Indicates that this date picker allows this date to be selected.
 * @param clock The system clock.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    onDismissRequest: () -> Unit,
    allowPastDates: Boolean = true,
    allowFutureDates: Boolean = true,
    allowToday: Boolean = true,
    clock: Clock = LocalClock.current,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long,
            ): Boolean {
                val today = LocalDate.now(clock)
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return when {
                    date.isBefore(today) -> allowPastDates
                    date.isAfter(today) -> allowFutureDates
                    else -> allowToday
                }
            }

            override fun isSelectableYear(
                year: Int,
            ): Boolean {
                val clockYear = clock.instant().atZone(clock.zone).year
                return when {
                    clockYear > year -> allowPastDates
                    clockYear < year -> allowFutureDates
                    else -> true
                }
            }
        },
    )
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .semantics {
                testTagsAsResourceId = true
                testTag = "DatePickerDialog"
            }
            .wrapContentHeight(),
    ) {
        val colors = bitwardenDatePickerColors()
        Surface(
            modifier = Modifier
                // The Date picker has specific requirements in order to look correct.
                // It does not follow the normal dialog size rules.
                .requiredWidth(width = 360.dp)
                .heightIn(max = 568.0.dp),
            shape = BitwardenTheme.shapes.dialog,
            color = colors.containerColor,
        ) {
            Column {
                // Wrap the content in a Box with a weight of 1f to ensure that any buttons
                // are not pushed out of view when running on small screens. Fill is false to
                // support collapsing the dialog's height when switching to input mode.
                Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
                    DatePicker(
                        modifier = Modifier.verticalScroll(state = rememberScrollState()),
                        state = datePickerState,
                        colors = colors,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                ) {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.cancel),
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag(tag = "DismissAlertButton"),
                    )
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.okay),
                        onClick = {
                            datePickerState
                                .selectedDateMillis
                                ?.let { millis ->
                                    Instant
                                        .ofEpochMilli(millis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                }
                                .let(onDateSelect)
                        },
                        modifier = Modifier.testTag(tag = "AcceptAlertButton"),
                    )
                }
            }
        }
    }
}

@Composable
private fun bitwardenDatePickerColors(): DatePickerColors = DatePickerColors(
    containerColor = BitwardenTheme.colorScheme.background.primary,
    titleContentColor = BitwardenTheme.colorScheme.text.secondary,
    headlineContentColor = BitwardenTheme.colorScheme.text.primary,
    weekdayContentColor = BitwardenTheme.colorScheme.text.secondary,
    subheadContentColor = BitwardenTheme.colorScheme.text.secondary,
    navigationContentColor = BitwardenTheme.colorScheme.icon.primary,
    yearContentColor = BitwardenTheme.colorScheme.text.primary,
    disabledYearContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    currentYearContentColor = BitwardenTheme.colorScheme.text.primary,
    selectedYearContentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    selectedYearContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    disabledSelectedYearContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledSelectedYearContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    dayContentColor = BitwardenTheme.colorScheme.text.primary,
    disabledDayContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledSelectedDayContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledSelectedDayContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    selectedDayContentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    selectedDayContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    todayContentColor = BitwardenTheme.colorScheme.text.primary,
    todayDateBorderColor = BitwardenTheme.colorScheme.filledButton.background,
    dividerColor = BitwardenTheme.colorScheme.stroke.divider,
    dayInSelectionRangeContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    dayInSelectionRangeContentColor = BitwardenTheme.colorScheme.text.primary,
    dateTextFieldColors = bitwardenTextFieldColors(
        focusedIndicatorColor = BitwardenTheme.colorScheme.outlineButton.border,
        unfocusedIndicatorColor = BitwardenTheme.colorScheme.outlineButton.border,
        disabledIndicatorColor = BitwardenTheme.colorScheme.outlineButton.border,
    ),
)

@Suppress("MagicNumber")
@Preview
@Composable
private fun BitwardenDatePickerDialog_preview() {
    BitwardenTheme {
        BitwardenDatePickerDialog(
            initialDate = LocalDate.of(2026, 5, 2),
            onDateSelect = {},
            onDismissRequest = {},
        )
    }
}
