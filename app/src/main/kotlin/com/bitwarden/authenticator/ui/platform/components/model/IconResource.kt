package com.bitwarden.authenticator.ui.platform.components.model

import androidx.compose.ui.graphics.painter.Painter

/**
 * Data class representing the resources required for an icon.
 *
 * @property iconPainter Painter for the icon.
 * @property contentDescription String for the icon's content description.
 * @property testTag The optional test tag to associate with this icon.
 */
data class IconResource(
    val iconPainter: Painter,
    val contentDescription: String,
    val testTag: String? = null,
)
