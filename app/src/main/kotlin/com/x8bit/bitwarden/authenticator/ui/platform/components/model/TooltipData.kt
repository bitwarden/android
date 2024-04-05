package com.x8bit.bitwarden.authenticator.ui.platform.components.model

/**
 * Data class representing the data needed to create a tooltip icon in a composable.
 *
 * @property onClick A lambda function that defines the action to be performed when the tooltip icon
 * is clicked.
 * @property contentDescription A text description of the icon for accessibility purposes.
 */
data class TooltipData(
    val onClick: () -> Unit,
    val contentDescription: String,
)
