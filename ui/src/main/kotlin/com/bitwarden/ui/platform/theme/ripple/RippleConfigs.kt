package com.bitwarden.ui.platform.theme.ripple

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme

/**
 * A default [RippleConfiguration] with the [RippleAlpha.focusedAlpha] cleared to disable the focus
 * state.
 */
internal val NoFocusRippleConfig: RippleConfiguration = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        draggedAlpha = RippleDefaults.RippleAlpha.draggedAlpha,
        focusedAlpha = 0f,
        hoveredAlpha = RippleDefaults.RippleAlpha.hoveredAlpha,
        pressedAlpha = RippleDefaults.RippleAlpha.pressedAlpha,
    ),
)

/**
 * Derives a standard [RippleConfiguration] with the color overridden using the
 * [BitwardenColorScheme].
 */
internal fun BitwardenColorScheme.toRippleConfig(): RippleConfiguration = RippleConfiguration(
    color = this.background.pressed,
)
