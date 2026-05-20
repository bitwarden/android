package com.x8bit.bitwarden.ui.tools.feature.send.model

/**
 * Handlers for the "Upgraded to Premium" action card shown on Send surfaces.
 *
 * Pass `null` to indicate the card should not be displayed.
 */
data class UpgradedToPremiumCardData(
    val onCardClick: () -> Unit,
    val onCardDismiss: () -> Unit,
)
