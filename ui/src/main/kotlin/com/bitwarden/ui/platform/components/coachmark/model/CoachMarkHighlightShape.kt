package com.bitwarden.ui.platform.components.coachmark.model

private const val ROUNDED_RECT_DEFAULT_RADIUS = 8f

/**
 * Defines the available shapes for a coach mark highlight.
 */
sealed class CoachMarkHighlightShape {
    /**
     * A rounded rectangle shape which has a radius to round the corners by.
     *
     * @property radius the radius to use to round the corners of the rectangle shape.
     * Defaults to [ROUNDED_RECT_DEFAULT_RADIUS]
     */
    data class RoundedRectangle(
        val radius: Float = ROUNDED_RECT_DEFAULT_RADIUS,
    ) : CoachMarkHighlightShape()

    /**
     * An oval-shaped highlight.
     */
    data object Oval : CoachMarkHighlightShape()
}
