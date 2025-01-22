package com.x8bit.bitwarden.ui.platform.components.coachmark.model

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.ui.geometry.Rect

/**
 * Represents a highlight within a coach mark sequence.
 *
 * @param T The type of the enum key used to identify the highlight.
 * @property key The unique key identifying this highlight.
 * @property highlightBounds The rectangular bounds of the area to highlight.
 * @property toolTipState The state of the tooltip associated with this highlight.
 * @property shape The shape of the highlight (e.g., square, oval).
 */
@OptIn(ExperimentalMaterial3Api::class)
data class CoachMarkHighlightState<T : Enum<T>>(
    val key: T,
    val highlightBounds: Rect?,
    val toolTipState: TooltipState,
    val shape: CoachMarkHighlightShape,
)
