package com.bitwarden.ui.platform.components.button.model

import androidx.compose.ui.graphics.painter.Painter
import com.bitwarden.ui.util.Text

/**
 * Represents the data required to render a button.
 *
 * @property label The text to be displayed on the button.
 * @property onClick A lambda function to be executed when the button is clicked.
 * @property icon An optional icon to be displayed with the button.
 * @property testTag A optional unique identifier for testing purposes.
 * @property isExternalLink Indicates that the button is an external link.
 * @property isEnabled Whether the button is enabled.
 */
data class BitwardenButtonData(
    val label: Text,
    val onClick: () -> Unit,
    val icon: Painter? = null,
    val testTag: String? = null,
    val isExternalLink: Boolean = false,
    val isEnabled: Boolean = true,
)
