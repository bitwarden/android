package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldButtonColors
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.orNow
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A custom composable representing a button that can display the date picker dialog.
 *
 * This composable displays an [OutlinedTextField] with a dropdown icon as a trailing icon.
 * When the field is clicked, a date picker dialog appears.
 *
 * @param label The displayed label.
 * @param currentZonedDateTime The currently displayed time.
 * @param formatPattern The pattern to format the displayed time.
 * @param onDateSelect The callback to be invoked when a new date is selected.
 * @param isEnabled Whether the button is enabled.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun BitwardenDateSelectButton(
    label: String,
    currentZonedDateTime: ZonedDateTime?,
    formatPattern: String,
    onDateSelect: (ZonedDateTime) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    val formattedDate by remember(currentZonedDateTime) {
        mutableStateOf(
            currentZonedDateTime
                ?.toFormattedPattern(formatPattern)
                ?: "mm/dd/yyyy",
        )
    }

    OutlinedTextField(
        modifier = modifier
            .clearAndSetSemantics {
                role = Role.DropdownList
                contentDescription = "$label, $formattedDate"
            }
            .clickable(
                enabled = isEnabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { shouldShowDialog = !shouldShowDialog },
            ),
        textStyle = BitwardenTheme.typography.bodyLarge,
        readOnly = true,
        label = { Text(text = label) },
        value = formattedDate,
        onValueChange = { },
        enabled = shouldShowDialog,
        trailingIcon = {
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_chevron_down),
                contentDescription = null,
            )
        },
        colors = bitwardenTextFieldButtonColors(),
    )

    if (shouldShowDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentZonedDateTime.orNow().toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            shape = BitwardenTheme.shapes.dialog,
            colors = bitwardenDatePickerColors(),
            onDismissRequest = { shouldShowDialog = false },
            confirmButton = {
                BitwardenTextButton(
                    label = stringResource(id = R.string.ok),
                    onClick = {
                        onDateSelect(
                            ZonedDateTime
                                .ofInstant(
                                    Instant.ofEpochMilli(
                                        requireNotNull(datePickerState.selectedDateMillis),
                                    ),
                                    ZoneOffset.UTC,
                                )
                                .withZoneSameLocal(currentZonedDateTime.orNow().zone),
                        )
                        shouldShowDialog = false
                    },
                    modifier = Modifier.testTag(tag = "AcceptAlertButton"),
                )
            },
            dismissButton = {
                BitwardenTextButton(
                    label = stringResource(id = R.string.cancel),
                    onClick = { shouldShowDialog = false },
                    modifier = Modifier.testTag(tag = "DismissAlertButton"),
                )
            },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "AlertPopup"
            },
        ) {
            DatePicker(
                state = datePickerState,
                colors = bitwardenDatePickerColors(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun bitwardenDatePickerColors(): DatePickerColors = DatePickerColors(
    containerColor = BitwardenTheme.colorScheme.background.primary,
    titleContentColor = BitwardenTheme.colorScheme.text.secondary,
    headlineContentColor = BitwardenTheme.colorScheme.text.primary,
    weekdayContentColor = BitwardenTheme.colorScheme.text.primary,
    subheadContentColor = BitwardenTheme.colorScheme.text.secondary,
    navigationContentColor = BitwardenTheme.colorScheme.icon.primary,
    yearContentColor = BitwardenTheme.colorScheme.text.primary,
    disabledYearContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    currentYearContentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    selectedYearContentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    disabledSelectedYearContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    selectedYearContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    disabledSelectedYearContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    dayContentColor = BitwardenTheme.colorScheme.text.primary,
    disabledDayContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    selectedDayContentColor = BitwardenTheme.colorScheme.text.reversed,
    disabledSelectedDayContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    selectedDayContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    disabledSelectedDayContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    todayContentColor = BitwardenTheme.colorScheme.outlineButton.foreground,
    todayDateBorderColor = BitwardenTheme.colorScheme.outlineButton.border,
    dayInSelectionRangeContainerColor = BitwardenTheme.colorScheme.filledButton.background,
    dividerColor = BitwardenTheme.colorScheme.stroke.divider,
    dayInSelectionRangeContentColor = BitwardenTheme.colorScheme.text.primary,
    dateTextFieldColors = bitwardenTextFieldColors(),
)
