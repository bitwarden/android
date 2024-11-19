package com.x8bit.bitwarden.ui.platform.components.stepper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.ZERO_WIDTH_CHARACTER
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions

/**
 * Displays a stepper that allows the user to increment and decrement an int value.
 *
 * @param label Label for the stepper.
 * @param value Value to display. Null will display nothing. Will be clamped to [range] before
 * display.
 * @param onValueChange callback invoked when the user increments or decrements the count. Note
 * that this will not be called if the attempts to move value outside of [range].
 * @param modifier Modifier.
 * @param range Range of valid values.
 * @param isIncrementEnabled whether or not the increment button should be enabled.
 * @param isDecrementEnabled whether or not the decrement button should be enabled.
 * @param textFieldReadOnly whether or not the text field should be read only. The stepper
 * increment and decrement buttons function regardless of this value.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
fun BitwardenStepper(
    label: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedRange<Int> = 1..Int.MAX_VALUE,
    isIncrementEnabled: Boolean = true,
    isDecrementEnabled: Boolean = true,
    textFieldReadOnly: Boolean = true,
    stepperActionsTestTag: String? = null,
) {
    val clampedValue = value?.coerceIn(range)
    if (clampedValue != value && clampedValue != null) {
        onValueChange(clampedValue)
    }
    val isAtRangeMinimum = clampedValue?.let { (it - 1) !in range } ?: true
    val isAtRangeMaximum = clampedValue?.let { (it + 1) !in range } ?: false
    BitwardenTextFieldWithActions(
        label = label,
        // We use the zero width character instead of an empty string to make sure label is shown
        // small and above the input
        value = clampedValue?.toString() ?: ZERO_WIDTH_CHARACTER,
        actionsTestTag = stepperActionsTestTag,
        actions = {
            BitwardenFilledIconButton(
                vectorIconRes = R.drawable.ic_minus,
                contentDescription = "\u2212",
                onClick = {
                    val decrementedValue = ((clampedValue ?: 0) - 1).coerceIn(range)
                    if (decrementedValue != clampedValue) {
                        onValueChange(decrementedValue)
                    }
                },
                isEnabled = isDecrementEnabled && !isAtRangeMinimum,
                modifier = Modifier.testTag("DecrementValue"),
            )
            BitwardenFilledIconButton(
                vectorIconRes = R.drawable.ic_plus,
                contentDescription = "+",
                onClick = {
                    val incrementedValue = ((clampedValue ?: 0) + 1).coerceIn(range)
                    if (incrementedValue != clampedValue) {
                        onValueChange(incrementedValue)
                    }
                },
                isEnabled = isIncrementEnabled && !isAtRangeMaximum,
                modifier = Modifier.testTag("IncrementValue"),
            )
        },
        readOnly = textFieldReadOnly,
        keyboardType = KeyboardType.Number,
        onValueChange = { newValue ->
            onValueChange(
                newValue
                    // Make sure the placeholder is gone, since it will mess up the int conversion
                    .replace(ZERO_WIDTH_CHARACTER, "")
                    .orNullIfBlank()
                    ?.let { it.toIntOrNull()?.coerceIn(range) ?: clampedValue }
                    ?: range.start,
            )
        },
        modifier = modifier,
    )
}
