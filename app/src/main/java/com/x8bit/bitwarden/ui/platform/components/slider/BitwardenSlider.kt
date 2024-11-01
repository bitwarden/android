package com.x8bit.bitwarden.ui.platform.components.slider

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.toDp
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.slider.color.bitwardenSliderColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom Bitwarden-themed slider.
 *
 * @param value The currently set value.
 * @param range The range of values allowed.
 * @param onValueChange Lambda callback for when the value changes and whether the change was from
 * user interaction or not.
 * @param modifier The [Modifier] to be applied to this radio button.
 * @param sliderTag The option test tag for the slider component.
 * @param valueTag The option test tag for the value field component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun BitwardenSlider(
    value: Int,
    range: ClosedRange<Int>,
    onValueChange: (value: Int, isUserInteracting: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sliderTag: String? = null,
    valueTag: String? = null,
) {
    val sliderValue by rememberUpdatedState(newValue = value.coerceIn(range = range))
    var labelTextWidth by remember { mutableStateOf(value = Dp.Unspecified) }
    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        TextField(
            value = sliderValue.toString(),
            textStyle = BitwardenTheme.typography.bodyLarge,
            readOnly = true,
            onValueChange = { },
            label = {
                Text(
                    text = stringResource(id = R.string.length),
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                        if (labelTextWidth == Dp.Unspecified) {
                            labelTextWidth = layoutCoordinates.size.width.toDp(density = density)
                        }
                    },
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = bitwardenTextFieldColors(
                disabledBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .onPreviewKeyEvent { keyEvent ->
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            onValueChange(sliderValue + 1, true)
                            true
                        }

                        Key.DirectionDown -> {
                            onValueChange(sliderValue - 1, true)
                            true
                        }

                        else -> false
                    }
                }
                .semantics { valueTag?.let { testTag = it } }
                .wrapContentWidth()
                // We want the width to be no wider than the label + 16dp on either side
                .width(width = 16.dp + labelTextWidth + 16.dp),
        )

        Slider(
            value = sliderValue.toFloat(),
            onValueChange = { newValue -> onValueChange(newValue.toInt(), true) },
            onValueChangeFinished = { onValueChange(sliderValue, false) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            steps = range.endInclusive - 1,
            colors = bitwardenSliderColors(),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = bitwardenSliderColors(),
                    thumbSize = DpSize(width = 20.dp, height = 20.dp),
                    modifier = Modifier.shadow(elevation = 2.dp, shape = CircleShape),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(height = 4.dp),
                    drawStopIndicator = null,
                    colors = bitwardenSliderColors(),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp,
                )
            },
            modifier = Modifier
                .focusProperties { canFocus = false }
                .semantics { sliderTag?.let { testTag = it } }
                .weight(weight = 1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSlider_preview() {
    BitwardenTheme {
        BitwardenSlider(
            value = 6,
            range = 0..10,
            onValueChange = { _, _ -> },
        )
    }
}
