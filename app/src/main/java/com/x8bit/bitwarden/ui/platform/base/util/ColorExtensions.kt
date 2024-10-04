package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A fractional luminance value beyond which we will consider the associated color to be light
 * enough to require a dark overlay to be used.
 */
private const val DARK_OVERLAY_LUMINANCE_THRESHOLD = 0.65f

/**
 * Returns `true` if the given [Color] would require a light color to be used in any kind of
 * overlay when high contrast is important.
 */
@Stable
val Color.isLightOverlayRequired: Boolean
    get() = this.luminanceWcag1 < DARK_OVERLAY_LUMINANCE_THRESHOLD

/**
 * Returns the luminance of the given color based on the
 * [WCAG 1.0 guidelines of 2000](https://www.w3.org/TR/AERT/#color-contrast), which give more
 * weight to the red and blue components relative to more recent WCAG 2.0 values used by
 * [Color.luminance].
 */
@Suppress("MagicNumber")
@Stable
private val Color.luminanceWcag1: Float
    get() = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)

/**
 * Returns a [Color] within the current theme that can safely be overlaid on top of the given
 * [Color].
 */
@Composable
fun Color.toSafeOverlayColor(): Color {
    val surfaceColor = BitwardenTheme.colorScheme.background.primary
    val onSurfaceColor = BitwardenTheme.colorScheme.text.primary
    val lightColor: Color
    val darkColor: Color
    if (surfaceColor.luminance() > onSurfaceColor.luminance()) {
        lightColor = surfaceColor
        darkColor = onSurfaceColor
    } else {
        lightColor = onSurfaceColor
        darkColor = surfaceColor
    }
    return if (this.isLightOverlayRequired) lightColor else darkColor
}
