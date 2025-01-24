package com.x8bit.bitwarden.ui.platform.components.stepper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledIconButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays a stepper that allows the user to increment and decrement an int value.
 *
 * @param label Label for the stepper.
 * @param value Value to display. Null will display nothing. Will be clamped to [range] before
 * display.
 * @param onValueChange callback invoked when the user increments or decrements the count. Note
 * that this will not be called if the attempts to move value outside of [range].
 * @param modifier Modifier.
 * @param supportingText An optional supporting text that will appear below the stepper.
 * @param range Range of valid values.
 * @param isIncrementEnabled whether or not the increment button should be enabled.
 * @param isDecrementEnabled whether or not the decrement button should be enabled.
 * @param textFieldReadOnly whether or not the text field should be read only. The stepper
 * increment and decrement buttons function regardless of this value.
 * @param cardStyle Indicates the type of card style to be applied.
 */
@Composable
fun BitwardenStepper(
    label: String?,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    range: ClosedRange<Int> = 1..Int.MAX_VALUE,
    isIncrementEnabled: Boolean = true,
    isDecrementEnabled: Boolean = true,
    textFieldReadOnly: Boolean = true,
    stepperActionsTestTag: String? = null,
    cardStyle: CardStyle? = null,
) {
    BitwardenStepper(
        modifier = modifier,
        label = label,
        value = value,
        onValueChange = onValueChange,
        supportingTextContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodySmall,
                    color = bitwardenTextFieldColors().focusedSupportingTextColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        range = range,
        isIncrementEnabled = isIncrementEnabled,
        isDecrementEnabled = isDecrementEnabled,
        textFieldReadOnly = textFieldReadOnly,
        stepperActionsTestTag = stepperActionsTestTag,
        cardStyle = cardStyle,
    )
}

/**
 * Displays a stepper that allows the user to increment and decrement an int value.
 *
 * @param label Label for the stepper.
 * @param value Value to display. Null will display nothing. Will be clamped to [range] before
 * display.
 * @param onValueChange callback invoked when the user increments or decrements the count. Note
 * that this will not be called if the attempts to move value outside of [range].
 * @param modifier Modifier.
 * @param supportingTextContent An optional supporting text composable that will appear below the
 * stepper.
 * @param range Range of valid values.
 * @param isIncrementEnabled whether or not the increment button should be enabled.
 * @param isDecrementEnabled whether or not the decrement button should be enabled.
 * @param textFieldReadOnly whether or not the text field should be read only. The stepper
 * increment and decrement buttons function regardless of this value.
 * @param cardStyle Indicates the type of card style to be applied.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun BitwardenStepper(
    label: String?,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingTextContent: @Composable (ColumnScope.() -> Unit)?,
    range: ClosedRange<Int> = 1..Int.MAX_VALUE,
    isIncrementEnabled: Boolean = true,
    isDecrementEnabled: Boolean = true,
    textFieldReadOnly: Boolean = true,
    stepperActionsTestTag: String? = null,
    cardStyle: CardStyle? = null,
) {
    val clampedValue = value?.coerceIn(range)
    if (clampedValue != value && clampedValue != null) {
        onValueChange(clampedValue)
    }
    val isAtRangeMinimum = clampedValue?.let { (it - 1) !in range } ?: true
    val isAtRangeMaximum = clampedValue?.let { (it + 1) !in range } ?: false
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardBackground(cardStyle = cardStyle)
            .cardPadding(cardStyle = cardStyle, vertical = 8.dp),
    ) {
        BitwardenTextFieldWithActions(
            label = label,
            value = clampedValue?.toString().orEmpty(),
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
                        .orNullIfBlank()
                        ?.let { it.toIntOrNull()?.coerceIn(range) ?: clampedValue }
                        ?: range.start,
                )
            },
            actionsPadding = PaddingValues(end = 12.dp),
        )
        supportingTextContent?.let {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = it,
            )
            Spacer(modifier = Modifier.height(height = 4.dp))
        }
    }
}
