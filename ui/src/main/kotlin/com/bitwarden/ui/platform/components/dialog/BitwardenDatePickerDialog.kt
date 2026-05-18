package com.bitwarden.ui.platform.components.dialog

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A custom composable representing a dialog that displays a date picker.
 *
 * @param initialDate The initial [LocalDate] to display.
 * @param onDateSelect The callback invoked with the selected [LocalDate] when the user confirms.
 * @param onDismissRequest The callback invoked when the dialog is dismissed.
 */
@Composable
fun BitwardenDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
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
        },
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.clear),
                onClick = { onDateSelect(null) },
                contentColor = BitwardenTheme.colorScheme.status.error,
                modifier = Modifier.testTag(tag = "ClearButton"),
            )
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.cancel),
                onClick = onDismissRequest,
                modifier = Modifier.testTag(tag = "DismissAlertButton"),
            )
        },
        shape = BitwardenTheme.shapes.dialog,
        colors = bitwardenDatePickerColors(),
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
            testTag = "DatePickerDialog"
        },
    ) {
        DatePicker(
            state = datePickerState,
            colors = bitwardenDatePickerColors(),
        )
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
