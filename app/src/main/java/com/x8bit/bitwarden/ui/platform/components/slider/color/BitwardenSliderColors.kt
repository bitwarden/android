package com.x8bit.bitwarden.ui.platform.components.slider.color

import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for sliders.
 */
@Composable
fun bitwardenSliderColors(): SliderColors = SliderColors(
    thumbColor = BitwardenTheme.colorScheme.sliderButton.knobBackground,
    activeTrackColor = BitwardenTheme.colorScheme.sliderButton.filled,
    activeTickColor = Color.Transparent,
    inactiveTrackColor = BitwardenTheme.colorScheme.sliderButton.unfilled,
    inactiveTickColor = Color.Transparent,
    disabledThumbColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledActiveTrackColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledActiveTickColor = Color.Transparent,
    disabledInactiveTrackColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledInactiveTickColor = Color.Transparent,
)
