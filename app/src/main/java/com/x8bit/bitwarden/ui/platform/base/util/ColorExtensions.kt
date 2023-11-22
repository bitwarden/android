package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * A fractional luminance value beyond which we will consider the associated color to be light
 * enough to require a dark overlay to be used.
 */
private const val DARK_OVERLAY_LUMINANCE_THRESHOLD = 0.65f

/**
 * Returns `true` if the given [Color] would require a light color to be used in any kind of
 * overlay when high contrast is important.
 */
val Color.isLightOverlayRequired: Boolean
    get() = this.luminance() < DARK_OVERLAY_LUMINANCE_THRESHOLD

/**
 * Returns a [Color] within the current theme that can safely be overlaid on top of the given
 * [Color].
 */
@Composable
fun Color.toSafeOverlayColor(): Color {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
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
