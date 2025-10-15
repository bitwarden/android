package com.bitwarden.ui.platform.components.button.model

import com.bitwarden.ui.util.Text

/**
 * Represents the data required to render a button.
 *
 * @param label The text to be displayed on the button.
 * @param onClick A lambda function to be executed when the button is clicked.
 * @param testTag A optional unique identifier for testing purposes.
 */
data class BitwardenButtonData(
    val label: Text,
    val onClick: () -> Unit,
    val testTag: String? = null,
)
