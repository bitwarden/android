package com.x8bit.bitwarden.ui.platform.components.model

import androidx.compose.ui.graphics.painter.Painter

/**
 * Data class representing the resources required for an icon.
 *
 * @property iconPainter Painter for the icon.
 * @property contentDescription String for the icon's content description.
 */
data class IconResource(
    val iconPainter: Painter,
    val contentDescription: String,
)
