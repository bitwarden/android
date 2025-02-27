package com.bitwarden.authenticator.ui.platform.components.stepper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.ZERO_WIDTH_CHARACTER
import com.bitwarden.authenticator.ui.platform.base.util.orNullIfBlank
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenTextFieldWithActions
import com.bitwarden.authenticator.ui.platform.components.icon.BitwardenIconButtonWithResource
import com.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter

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
@Suppress("LongMethod")
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
    increaseButtonTestTag: String? = null,
    decreaseButtonTestTag: String? = null,
) {
    val clampedValue = value?.coerceIn(range)
    if (clampedValue != value && clampedValue != null) {
        onValueChange(clampedValue)
    }
    BitwardenTextFieldWithActions(
        label = label,
        // We use the zero width character instead of an empty string to make sure label is shown
        // small and above the input
        value = clampedValue
            ?.toString()
            ?: ZERO_WIDTH_CHARACTER,
        actionsTestTag = stepperActionsTestTag,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = rememberVectorPainter(id = R.drawable.ic_minus),
                    contentDescription = "\u2212",
                ),
                onClick = {
                    val decrementedValue = ((value ?: 0) - 1).coerceIn(range)
                    if (decrementedValue != value) {
                        onValueChange(decrementedValue)
                    }
                },
                isEnabled = isDecrementEnabled,
                modifier = Modifier.semantics {
                    if (decreaseButtonTestTag != null) {
                        testTag = decreaseButtonTestTag
                    }
                },
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = rememberVectorPainter(id = R.drawable.ic_plus),
                    contentDescription = "+",
                ),
                onClick = {
                    val incrementedValue = ((value ?: 0) + 1).coerceIn(range)
                    if (incrementedValue != value) {
                        onValueChange(incrementedValue)
                    }
                },
                isEnabled = isIncrementEnabled,
                modifier = Modifier.semantics {
                    if (increaseButtonTestTag != null) {
                        testTag = increaseButtonTestTag
                    }
                },
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
                    ?.let { it.toIntOrNull()?.coerceIn(range) ?: value }
                    ?: range.start,
            )
        },
        modifier = modifier,
    )
}
