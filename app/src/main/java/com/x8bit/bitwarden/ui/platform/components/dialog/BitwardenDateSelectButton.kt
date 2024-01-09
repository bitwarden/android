package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.x8bit.bitwarden.R
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
 * @param currentZonedDateTime The currently displayed time.
 * @param formatPattern The pattern to format the displayed time.
 * @param onDateSelect The callback to be invoked when a new date is selected.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenDateSelectButton(
    currentZonedDateTime: ZonedDateTime,
    formatPattern: String,
    onDateSelect: (ZonedDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    val formattedDate by remember(currentZonedDateTime) {
        mutableStateOf(currentZonedDateTime.toFormattedPattern(formatPattern))
    }
    // TODO: This should be "Date" but we don't have that string (BIT-1405)
    val label = stringResource(id = R.string.deletion_date)

    OutlinedTextField(
        modifier = modifier
            .clearAndSetSemantics {
                role = Role.DropdownList
                contentDescription = "$label, $formattedDate"
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { shouldShowDialog = !shouldShowDialog },
            ),
        textStyle = MaterialTheme.typography.bodyLarge,
        readOnly = true,
        label = { Text(text = label) },
        value = formattedDate,
        onValueChange = { },
        enabled = shouldShowDialog,
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_region_select_dropdown),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    if (shouldShowDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentZonedDateTime.toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { shouldShowDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateSelect(
                            ZonedDateTime
                                .ofInstant(
                                    Instant.ofEpochMilli(
                                        requireNotNull(datePickerState.selectedDateMillis),
                                    ),
                                    ZoneOffset.UTC,
                                )
                                .withZoneSameLocal(currentZonedDateTime.zone),
                        )
                        shouldShowDialog = false
                    },
                    modifier = modifier,
                ) {
                    Text(
                        text = stringResource(id = R.string.ok),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { shouldShowDialog = false },
                    modifier = modifier,
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
