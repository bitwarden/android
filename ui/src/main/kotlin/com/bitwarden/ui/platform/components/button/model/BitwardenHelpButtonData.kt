package com.bitwarden.ui.platform.components.button.model

/**
 * Data class representing the data needed to create a tooltip icon in a composable.
 *
 * @property onClick A lambda function that defines the action to be performed when the tooltip icon
 * is clicked.
 * @property contentDescription A text description of the icon for accessibility purposes.
 * @property isExternalLink Indicates that this button will launch an external link.
 */
data class BitwardenHelpButtonData(
    val onClick: () -> Unit,
    val contentDescription: String,
    val isExternalLink: Boolean,
)
