package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconResource

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
 */
@Composable
fun BitwardenStepper(
    label: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedRange<Int> = 1..Int.MAX_VALUE,
    isIncrementEnabled: Boolean = true,
    isDecrementEnabled: Boolean = true,
) {
    val clampedValue = value?.coerceIn(range)
    if (clampedValue != value && clampedValue != null) {
        onValueChange(clampedValue)
    }
    BitwardenTextFieldWithActions(
        label = label,
        // we use a space instead of empty string to make sure label is shown small and above
        // the input
        value = clampedValue
            ?.toString()
            ?: " ",
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "\u2212",
                ),
                onClick = {
                    val decrementedValue = ((value ?: 0) - 1).coerceIn(range)
                    if (decrementedValue != value) {
                        onValueChange(decrementedValue)
                    }
                },
                isEnabled = isDecrementEnabled,
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "+",
                ),
                onClick = {
                    val incrementedValue = ((value ?: 0) + 1).coerceIn(range)
                    if (incrementedValue != value) {
                        onValueChange(incrementedValue)
                    }
                },
                isEnabled = isIncrementEnabled,
            )
        },
        readOnly = true,
        onValueChange = {},
        modifier = modifier,
    )
}
