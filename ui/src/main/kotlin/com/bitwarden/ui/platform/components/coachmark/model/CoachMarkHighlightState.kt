package com.bitwarden.ui.platform.components.coachmark.model

import androidx.compose.ui.geometry.Rect
import com.bitwarden.ui.platform.components.tooltip.model.BitwardenToolTipState

/**
 * Represents a highlight within a coach mark sequence.
 *
 * @param T The type of the enum key used to identify the highlight.
 * @property key The unique key identifying this highlight.
 * @property highlightBounds The rectangular bounds of the area to highlight.
 * @property toolTipState The state of the tooltip associated with this highlight.
 * @property shape The shape of the highlight (e.g., square, oval).
 */
data class CoachMarkHighlightState<T : Enum<T>>(
    val key: T,
    val highlightBounds: Rect?,
    val toolTipState: BitwardenToolTipState,
    val shape: CoachMarkHighlightShape,
)
