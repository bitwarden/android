package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.orNow
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import java.time.ZonedDateTime

/**
 * A custom composable representing a button that can display the time picker dialog.
 *
 * This composable displays an [OutlinedTextField] with a dropdown icon as a trailing icon.
 * When the field is clicked, a time picker dialog appears.
 *
 * @param label The displayed label.
 * @param currentZonedDateTime The currently displayed time.
 * @param formatPattern The pattern to format the displayed time.
 * @param onTimeSelect The callback to be invoked when a new time is selected.
 * @param isEnabled Whether the button is enabled.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param is24Hour Indicates if the time selector should use a 24 hour format or a 12 hour format
 * with AM/PM.
 */
@Composable
fun BitwardenTimeSelectButton(
    label: String,
    currentZonedDateTime: ZonedDateTime?,
    formatPattern: String,
    onTimeSelect: (hour: Int, minute: Int) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = false,
) {
    var shouldShowDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    val formattedTime by remember(currentZonedDateTime) {
        mutableStateOf(
            currentZonedDateTime
                ?.toFormattedPattern(formatPattern)
                ?: "--:-- --",
        )
    }
    OutlinedTextField(
        modifier = modifier
            .clearAndSetSemantics {
                role = Role.DropdownList
                contentDescription = "$label, $formattedTime"
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
        value = formattedTime,
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
        BitwardenTimePickerDialog(
            initialHour = currentZonedDateTime.orNow().hour,
            initialMinute = currentZonedDateTime.orNow().minute,
            onTimeSelect = { hour, minute ->
                shouldShowDialog = false
                onTimeSelect(hour, minute)
            },
            onDismissRequest = { shouldShowDialog = false },
            is24Hour = is24Hour,
        )
    }
}
